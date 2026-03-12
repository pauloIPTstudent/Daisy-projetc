# End Points

| Endpoint               | Método | Função                                |
| ---------------------- | ------ | ------------------------------------- |
| `/register_sensor`     | POST   | Associa sensor a um user e gera token |
| `/assign_sensor_plant` | POST   | Associa sensor a uma planta do user   |
| `/list_sensors`        | GET    | Lista todos sensores de um user       |
| `/validate_sensor`     | POST   | Valida token do sensor (ESP32)        |
| `/sensor_reading`      | POST   | Recebe leituras do sensor             |
