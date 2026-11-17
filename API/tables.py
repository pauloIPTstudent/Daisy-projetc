from flask import Flask
import enum
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

    @staticmethod
    def search_plants_by_user(user_id, search_query, limit=5):
        # Cria o padrão de busca (ex: "samambaia" vira "%samambaia%")
        search_pattern = f"%{search_query}%"
        
        return Plant.query.filter(
            Plant.user_id == user_id,
            (Plant.name.ilike(search_pattern)) | (Plant.specie.ilike(search_pattern))
        ).limit(limit).all()

class SensorStatus(enum.Enum):
    OK = "ok"
    UNSTABLE = "unstable"
    FAILURE = "failure"

# Criando uma tabela (modelo)
class Sensor(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(120), nullable=True)
    mac = db.Column(db.String(120), nullable=False)
    token = db.Column(db.String(200), nullable=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=True)
    plant_id = db.Column(db.Integer, db.ForeignKey('plant.id'), nullable=True)
    battery = db.Column(db.Integer, nullable=True)
    light_sensor_status = db.Column(db.Enum(SensorStatus), default=SensorStatus.OK, nullable=True)    
    temperature_sensor_status = db.Column(db.Enum(SensorStatus), default=SensorStatus.OK, nullable=True)

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "mac": self.mac,
            "battery": self.battery,
            "plant_id": self.plant_id,
            # Importante: tratar o Enum para string
            "light_sensor_status": self.light_sensor_status.value if self.light_sensor_status else None,
            "temperature_sensor_status": self.temperature_sensor_status.value if self.temperature_sensor_status else None,
        }
    
    @staticmethod
    def associate_sensor(mac,user_id):
        sensor = Sensor.query.filter_by(mac=mac).first()
        if (sensor==None) :
            sensor = Sensor(mac=mac)
            db.session.add(sensor)
            db.session.commit()
        
        sensor.token = secrets.token_urlsafe(32)
        sensor.user_id = user_id
        db.session.commit()
        return sensor
    
    @staticmethod
    def get_sensor_by_token(token):
        if not token:
            return None
        return Sensor.query.filter_by(token=token).first()
    
    @staticmethod
    def associate_plant_to_sensor(mac,plant_id):
        sensor = Sensor.query.filter_by(mac=mac).first()
        if sensor :
            sensor.plant_id = plant_id
            db.session.commit()
            return sensor
        
# Leituras de sensores (humidade do solo e intensidade de luz) associadas a uma planta
class Reading(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    sensor_id = db.Column(db.Integer, db.ForeignKey('sensor.id'), nullable=False)
    plant_id = db.Column(db.Integer, db.ForeignKey('plant.id'), nullable=True)
    timestamp = db.Column(db.DateTime, default=datetime.utcnow, nullable=False)
    humidity = db.Column(db.Float, nullable=True)
    light = db.Column(db.Float, nullable=True)
    temperature = db.Column(db.Float, nullable=True)
    #plant = db.relationship('Plant', backref=db.backref('readings', lazy=True))

    def to_dict(self):
        return {
            "id": self.id,
            "sensor_id": self.sensor_id,
            "plant_id": self.plant_id,
            "timestamp": self.timestamp,
            "humidity": self.humidity,
            "light": self.light,
            "temperature": self.temperature,
        }

    @staticmethod
    def create_reading(sensor_id,plant_id=None, humidity=None, light=None,temperature=None):
        reading = Reading(sensor_id=sensor_id, plant_id=plant_id, humidity=humidity, light=light, temperature=temperature, timestamp=datetime.now(timezone.utc))
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
    
    @staticmethod
    def get_readings_by_sensor_timeframe(sensor_id, start_time, end_time):
        return Reading.query.filter(
            Reading.sensor_id == sensor_id,
            Reading.timestamp >= start_time,
            Reading.timestamp <= end_time
        ).all()
    
    @staticmethod
    def last_reading_by_sensor(sensor_id):
        """Retorna a leitura mais recente de um sensor específico."""
        return Reading.query.filter_by(sensor_id=sensor_id)\
            .order_by(Reading.timestamp.desc())\
            .first()

    @staticmethod
    def last_reading_by_plant(plant_id):
        """Retorna a leitura mais recente de uma planta específica."""
        return Reading.query.filter_by(plant_id=plant_id)\
            .order_by(Reading.timestamp.desc())\
            .first()

# Criando o banco e tabelas
with app.app_context():
    db.create_all()
