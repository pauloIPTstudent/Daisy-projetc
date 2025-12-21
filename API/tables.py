from flask import Flask
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime, timezone
import secrets
from flask_sqlalchemy import SQLAlchemy

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///database.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)

# Criando uma tabela (modelo)
class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password = db.Column(db.String(200), nullable=False)
    token = db.Column(db.String(200), nullable=False)

    @staticmethod
    def create_account(email, password):
        user = User(email=email)
        user.set_password(password)
        user.token = secrets.token_urlsafe(32)
        db.session.add(user)
        db.session.commit()
        return user
    
    def set_password(self, password):
        self.password = generate_password_hash(password)


    def get_token(self):
        return self.token

    def check_token(self, token):
        return self.token == token

    def check_password(self, password):
        return check_password_hash(self.password, password)
    

    
    @staticmethod
    def get_user_by_token(token):
        if not token:
            return None
        return User.query.filter_by(token=token).first()

    # Cada usuário pode ter várias plantas; cada planta pode ter um sensor (1:1)
class Plant(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(120), nullable=False)
    specie = db.Column(db.String(120))
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)

    @staticmethod
    def create_plant(name, specie, user_id):
        plant = Plant(name=name, specie=specie, user_id=user_id)
        db.session.add(plant)
        db.session.commit()
        return plant
    
    @staticmethod
    def list_plants_by_user_paginated(user_token, page=1, per_page=10):
        user = User.query.filter_by(token=user_token).first()
        if not user:
            return False
        return Plant.query.filter_by(user_id=user.id).paginate(page=page, per_page=per_page, error_out=False)
    
    @staticmethod
    def edit_plant_user_token(plant_id, user_token, new_name, new_specie):
        user = User.query.filter_by(token=user_token).first()
        if not user:
            return False
        plant = Plant.query.filter_by(id=plant_id, user_id=user.id).first()
        if plant:
            plant.name = new_name
            plant.specie = new_specie
            db.session.commit()
            return True
        return False
    
    @staticmethod
    def delete_plant_user(plant_id, user_token):
        user = User.query.filter_by(token=user_token).first()
        if not user:
            return False
        plant = Plant.query.filter_by(id=plant_id, user_id=user.id).first()
        if plant:
            db.session.delete(plant)
            db.session.commit()
            return True
        return False

# Leituras de sensores (humidade do solo e intensidade de luz) associadas a uma planta
class Reading(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    plant_id = db.Column(db.Integer, db.ForeignKey('plant.id'), nullable=False)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow, nullable=False)
    humidity = db.Column(db.Float, nullable=True)
    light = db.Column(db.Float, nullable=True)

    plant = db.relationship('Plant', backref=db.backref('readings', lazy=True))

    @staticmethod
    def create_reading(plant_id, humidity=None, light=None):
        reading = Reading(plant_id=plant_id, humidity=humidity, light=light, timestamp=datetime.now(timezone.utc))
        db.session.add(reading)
        db.session.commit()
        return reading

    @staticmethod
    def get_readings_by_plant_timeframe(plant_id, start_time, end_time):
        return Reading.query.filter(
            Reading.plant_id == plant_id,
            Reading.timestamp >= start_time,
            Reading.timestamp <= end_time
        ).all()
    
    
# Criando o banco e tabelas
with app.app_context():
    db.create_all()
