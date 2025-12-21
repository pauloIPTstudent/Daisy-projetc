from flask import request, jsonify
import re
import secrets
import os
from datetime import datetime, timezone

from tables import app, db, User, Plant, Reading


def get_token_from_request(req):
    auth = req.headers.get('Authorization')
    if auth and auth.lower().startswith('bearer '):
        return auth.split(None, 1)[1].strip()
    # fallback to JSON body
    data = req.get_json(silent=True) or {}
    return data.get('token')


def parse_iso(s):
    if not s:
        return None
    try:
        if s.endswith('Z'):
            return datetime.fromisoformat(s.replace('Z', '+00:00')).astimezone(timezone.utc)
        return datetime.fromisoformat(s)
    except Exception:
        return None


@app.route('/create_account', methods=['POST'])
def create_account():
    data = request.get_json() or {}
    email = data.get('email')
    password = data.get('password')
    if not email or not password:
        return jsonify({'error': 'email and password required'}), 400

    # simple email validation
    if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
        return jsonify({'error': 'invalid email'}), 400

    if User.query.filter_by(email=email).first():
        return jsonify({'error': 'email already registered'}), 400

    user = User().create_account(email, password)
    return jsonify({'token': user.get_token()}), 201


@app.route('/login', methods=['POST'])
def login():
    data = request.get_json() or {}
    email = data.get('email')
    password = data.get('password')
    if not email or not password:
        return jsonify({'error': 'email and password required'}), 400

    user = User.query.filter_by(email=email).first()
    if not user:
        return jsonify({'error': 'invalid email'}), 401
    if not user.check_password(password):
        return jsonify({'error': 'invalid password'}), 401

    # ensure token exists
    if not user.token:
        user.token = secrets.token_urlsafe(32)
        db.session.commit()

    return jsonify({'token': user.token}), 200


@app.route('/listuserplants', methods=['GET'])
def list_user_plants():
    token = get_token_from_request(request) or request.args.get('token')
    user = User.get_user_by_token(token)
    if not user:
        return jsonify({'error': 'invalid token'}), 401

    plants = Plant.query.filter_by(user_id=user.id).all()
    result = [{'id': p.id, 'name': p.name, 'specie': p.specie} for p in plants]
    return jsonify({'plants': result}), 200


@app.route('/createplant', methods=['POST'])
def create_plant():
    data = request.get_json() or {}
    token = get_token_from_request(request) or data.get('token')
    name = data.get('name')
    specie = data.get('specie')

    if not token or not name:
        return jsonify({'error': 'token and plant name required'}), 400

    user = User.get_user_by_token(token)
    if not user:
        return jsonify({'error': 'invalid token'}), 401

    plant = Plant.create_plant(name, specie, user.id)
    return jsonify({'success': True, 'id': plant.id}), 201


@app.route('/readings', methods=['GET'])
def get_readings():
    # params: plant_id (required), start_time (ISO), end_time (ISO)
    #token = get_token_from_request(request) or request.args.get('token')
    plant_id = request.args.get('plant_id') or (request.get_json(silent=True) or {}).get('plant_id')
    start_ts = request.args.get('start_time') or (request.get_json(silent=True) or {}).get('start_time')
    end_ts = request.args.get('end_time') or (request.get_json(silent=True) or {}).get('end_time')

    #if not token or not plant_id:
    #    return jsonify({'error': 'token and plant_id required'}), 400

    #user = User.get_user_by_token(token)
    #if not user:
    #    return jsonify({'error': 'invalid token'}), 401

    plant = Plant.query.filter_by(id=plant_id).first()
    if not plant:
        return jsonify({'error': 'plant not found or not owned by user'}), 404

    start_dt = parse_iso(start_ts) if start_ts else None
    if start_ts and start_dt is None:
        return jsonify({'error': 'invalid start_time format, use ISO (e.g. 2025-12-01T00:00:00Z)'}), 400
    end_dt = parse_iso(end_ts) if end_ts else None
    if end_ts and end_dt is None:
        return jsonify({'error': 'invalid end_time format, use ISO (e.g. 2025-12-21T23:59:59Z)'}), 400

    if start_dt and end_dt and start_dt > end_dt:
        return jsonify({'error': 'start_time must be before or equal to end_time'}), 400

    readings = Reading.get_readings_by_plant_timeframe(plant.id, start_dt, end_dt)
    result = [
        {
            'id': r.id,
            'timestamp': r.timestamp.isoformat(),
            'humidity': r.humidity,
            'light': r.light,
        }
        for r in readings
    ]
    return jsonify({'readings': result}), 200


@app.route('/editplant', methods=['PUT'])
def edit_plant():
    data = request.get_json() or {}
    token = get_token_from_request(request) or data.get('token')
    plant_id = data.get('id')
    new_name = data.get('name')
    new_specie = data.get('specie')

    if not token or not plant_id:
        return jsonify({'error': 'token and plant id required'}), 400

    user = User.get_user_by_token(token)
    if not user:
        return jsonify({'error': 'invalid token'}), 401

    plant = Plant.query.filter_by(id=plant_id, user_id=user.id).first()
    if not plant:
        return jsonify({'error': 'plant not found'}), 404

    Plant.edit_plant_user_token(plant_id, token, new_name, new_specie)
    return jsonify({'success': True}), 200


@app.route('/deleteplant', methods=['DELETE'])
def delete_plant():
    data = request.get_json() or {}
    token = get_token_from_request(request) or data.get('token')
    plant_id = data.get('id')

    if not token or not plant_id:
        return jsonify({'error': 'token and plant id required'}), 400

    user = User.get_user_by_token(token)
    if not user:
        return jsonify({'error': 'invalid token'}), 401

    plant = Plant.query.filter_by(id=plant_id, user_id=user.id).first()
    if not plant:
        return jsonify({'error': 'plant not found'}), 404

    Plant.delete_plant_user(plant_id, token)
    return jsonify({'success': True}), 200


@app.route('/sensor_reading', methods=['POST'])
def sensor_reading():
    data = request.get_json() or {}
    plant_id = data.get('plant_id')
    humidity = data.get('humidity')
    light = data.get('light')

    if not plant_id:
        return jsonify({'error': 'plant_id required'}), 400

    plant = Plant.query.filter_by(id=plant_id).first()
    if not plant:
        return jsonify({'error': 'plant not found'}), 404

    Reading.create_reading(plant_id, humidity, light)
    return jsonify({'success': True}), 201


if __name__ == '__main__':
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)

