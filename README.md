<div align="center"><img width="186" height="69" alt="Captura de ecrã_2026-11-17_17-41-48" src="https://github.com/user-attachments/assets/24158d1e-61ae-4a21-a846-d9fa87c9a08d" /></div>
<br/>
<div align="">

![ESP32](https://img.shields.io/badge/ESP32-ESP--IDF%20%2F%20RTOS-E7352C?style=for-the-badge&logo=espressif&logoColor=white)
![Python](https://img.shields.io/badge/Backend-Python%20%2F%20Flask-3776AB?style=for-the-badge&logo=python&logoColor=white)
![Android](https://img.shields.io/badge/Mobile-Android%20Studio-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![PlatformIO](https://img.shields.io/badge/IoT-Platform.io-6C8EBF?style=for-the-badge&logo=platformio&logoColor=white)
![Render](https://img.shields.io/badge/Cloud-Render-46E3B7?style=for-the-badge&logo=render&logoColor=black)

</div>




# Daisy System: Smart Plant Monitoring IoT Ecosystem

O **Daisy System** revoluciona o cuidado das plantas ao conectar hardware embarcado de alta performance a uma aplicação móvel intuitiva, centralizando a telemetria e o estado do seu jardim em tempo real.

https://github.com/user-attachments/assets/fb6b54b4-cd04-4014-a4a6-05517114090a



## Novidades e Evolução (O que elevou o nível)

Este projeto passou por uma evolução arquitetural profunda para refletir padrões de engenharia de nível industrial:

* **Arquitetura Concorrente em Tempo Real:** Firmware embarcado no ESP32 desenvolvido em ESP-IDF/FreeRTOS com tarefas paralelas e comunicação segura via HTTPS/TLS.
* **Autenticação e Segurança de Dispositivos:** Gestão robusta de identidade por tokens e armazenamento NVS para parelhamento seguro entre o hardware e a nuvem.
* **Provisionamento Wi-Fi via AP Local:** Configuração inicial feita diretamente pelo servidor web integrado no ESP32 (modo *Access Point*), que transmite as credenciais de rede e tokens de autenticação para o dispositivo antes de fechar o serviço e transitar para o modo de operação normal.
* **Integração com Inteligência Artificial:** Reconhecimento botânico automático de espécies através de imagens com consumo de API externa (PlantNet).
* **Telemetria e Dados em Nuvem:** Backend estruturado em Python/Flask com persistência relacional de métricas de temperatura, luz, humidade e nível de bateria.
* **Experiência Mobile Completa:** Aplicação Android nativa com painéis dinâmicos, gráficos de histórico temporal e sincronização de dados meteorológicos em tempo real.


## Galeria da Aplicação (Screenshots)

<div align="center">

| Ecrã Principal & Clima | Gestão do Jardim | Detalhes da Planta & Sensores | Gráficos de Telemetria |
| :---: | :---: | :---: | :---: |
| ![Home](https://github.com/user-attachments/assets/eb887a0f-1bbc-43c4-8949-3b911ab0e2be) | ![Jardim](https://github.com/user-attachments/assets/d3e2a35d-5ac0-490b-8eb4-208a53c0a60e) | ![Detalhes](https://github.com/user-attachments/assets/b94ece6f-813d-4e0e-9dd1-6bd2f9e8615b) | ![Historico](https://github.com/user-attachments/assets/b9c49946-4054-4a29-ac8c-157ab52846c4) |

</div>



## Estrutura de Pastas do Projeto

```text
├──  ESP32-IoT-main/       # Código embarcado em C/C++ (ESP-IDF / FreeRTOS)
├──  API/          # Servidor REST em Python (Flask) e integração com nuvem
└──  DaisyAPP/          # Aplicação nativa mobile desenvolvida em Android Studio
```

---

<details>
<summary>🎬 <b>Bónus: Demonstração em Tempo Real e Inteligência Artificial</b> <i>(Clique para assistir)</i></summary>

<br>

> *"Ver para crer: acompanhe a telemetria dos sensores ao vivo e o reconhecimento botânico instantâneo."*

<div align="center">

| Ver Detalhes da Planta & Sensores | Identificar Planta com AI |
| :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------: |
| https://github.com/user-attachments/assets/409a8f8e-d21d-4d37-9fe1-1371d8b31ca5 | https://github.com/user-attachments/assets/db727de2-5fb1-45b5-8424-367b64138e7d |

</div>

</details>

<details>
<summary>✨ <b>Bónus: Guia Passo a Passo (Configuração Inicial do Dispositivo IoT)</b> <i>(Clique para ver como é simples)</i></summary>

<br>

> *"Configurar hardware não precisa ser complicado. Do tirar da caixa até à primeira leitura na nuvem, são literalmente segundos."*

Quando o **Daisy Sensor** liga pela primeira vez, ele cria automaticamente a sua própria rede local (*Access Point*), então app, de forma seamless, guia o utilizador passo a passo diretamente, sem cabos e sem terminais:

<div align="center">

| 1. Ativar o Modo de Parelhamento | 2. Selecionar o Dispositivo | 3. Enviar Credenciais & Pronto! |
| :---: | :---: | :---: |
| <img width="720" height="1650" alt="image26" src="https://github.com/user-attachments/assets/09ce7266-aa5a-42e0-88c8-a13b2765d9e2" />| <img width="720" height="1650" alt="image27" src="https://github.com/user-attachments/assets/aa935ffb-d469-436b-8911-64dba166161e" /> | <img width="720" height="1650" alt="image28" src="https://github.com/user-attachments/assets/f87d1700-c3e7-4e7e-803c-9d5678501332" /> |
| *A app orienta a ativação do sensor* | *Deteção automática do ponto de acesso* | *Wi-Fi e token seguros enviados em segundos* |

</div>

</details>
