#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h> // Necessário para notificações

// UUIDs (Devem ser EXATAMENTE os mesmos no seu código Kotlin)
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_READ "beb5483e-36e1-4688-b7f5-ea07361b26a8" 
#define CHARACTERISTIC_NAME "82c8bb2a-4309-11ec-81d3-0242ac130003" 

BLECharacteristic *pReadCharacteristic;
BLECharacteristic *pNameCharacteristic;
bool deviceConnected = false;
String sensorName = "Meu Sensor de Planta";

// --- CALLBACKS DE CONEXÃO ---
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("App Android Conectado!");
    };
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("App Android Desconectado. Reiniciando anúncio...");
      pServer->getAdvertising()->start(); // Reinicia o sinal para poder conectar de novo
    }
};

// --- CALLBACKS PARA TROCA DE NOME ---
class MyNameCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      String value = pCharacteristic->getValue(); // Correção: usando String do Arduino

      if (value.length() > 0) {
        sensorName = value;
        Serial.print("Novo nome configurado pelo App: ");
        Serial.println(sensorName);
        
        // Dica: Se quiser salvar o nome permanentemente, usaria a biblioteca Preferences.h aqui.
      }
    }
};

void setup() {
  Serial.begin(115200);

  // Inicializa o dispositivo BLE
  BLEDevice::init(sensorName.c_str());
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Cria o Serviço
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // 1. Característica de LEITURA DO SENSOR
  pReadCharacteristic = pService->createCharacteristic(
                          CHARACTERISTIC_READ,
                          BLECharacteristic::PROPERTY_READ | 
                          BLECharacteristic::PROPERTY_NOTIFY
                        );
  // Adiciona o descritor para permitir notificações (importante para o Android)
  pReadCharacteristic->addDescriptor(new BLE2902());

  // 2. Característica do NOME
  pNameCharacteristic = pService->createCharacteristic(
                          CHARACTERISTIC_NAME,
                          BLECharacteristic::PROPERTY_READ | 
                          BLECharacteristic::PROPERTY_WRITE
                        );
  pNameCharacteristic->setCallbacks(new MyNameCallbacks());
  pNameCharacteristic->setValue(sensorName.c_str());

  // Inicia o serviço
  pService->start();

  // Configura e inicia o anúncio (Advertising)
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->start();
  
  Serial.println("Aguardando conexão do Android...");
}

void loop() {
  if (deviceConnected) {
    // Simula a leitura do sensor (Pino 34 ou valor aleatório para teste)
    String payload = "l,98,28,6.5,75";

    // Atualiza o valor para o App ler
    pReadCharacteristic->setValue(payload.c_str());
    
    // Opcional: Notifica o App automaticamente a cada 5 segundos
    pReadCharacteristic->notify();
    
    Serial.print("Enviando leitura: ");
    Serial.println(payload);
    
    delay(5000); 
  }
}