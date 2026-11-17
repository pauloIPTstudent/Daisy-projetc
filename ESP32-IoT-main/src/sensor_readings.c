#include "driver/adc.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "sensor_readings.h"
#include "semaphore_manager.h"
#include "buffer_manager.h"

#define VIN 3.3       // Tensão de entrada do ESP32
#define ADC_RES 4095.0 // Resolução para 12 bits
#define R_FIXED 10000.0 // Resistor de 10k ohms usado no divisor

void sensor_readings_task(void *pvParameters)
{
    // Configura o ADC1 canal 0 (GPIO36, por exemplo)
    adc1_config_width(ADC_WIDTH_BIT_12);         // 12 bits de resolução (0-4095)
    adc1_config_channel_atten(ADC1_CHANNEL_4, ADC_ATTEN_DB_0); // Atenuação 0dB

    while (1) {
        xSemaphoreTake(statrt_readings, portMAX_DELAY);
        int valor = 0;
        for(int i= 0;i<3;i++){
            valor += adc1_get_raw(ADC1_CHANNEL_4); // Lê valor bruto do ADC

        }
        valor/=3;
        float v_out = (valor * VIN) / ADC_RES;
        float r_ldr = (R_FIXED * (VIN - v_out)) / v_out;
        float lux = 500000.0 / r_ldr;
        ESP_LOGI("LDR", "Valor LUX: %d", lux);
        xQueueSend(reading_buffer, &valor, pdMS_TO_TICKS(100));
        vTaskDelay(pdMS_TO_TICKS(1000)); // espera 1 segundo
    }
}



/*
| ADC1 Channel   | Pino GPIO |
| -------------- | --------- |
| ADC1_CHANNEL_0 | GPIO36    |
| ADC1_CHANNEL_1 | GPIO37    |
| ADC1_CHANNEL_2 | GPIO38    |
| ADC1_CHANNEL_3 | GPIO39    |
| ADC1_CHANNEL_4 | GPIO32    |
| ADC1_CHANNEL_5 | GPIO33    |
| ADC1_CHANNEL_6 | GPIO34    |
| ADC1_CHANNEL_7 | GPIO35    |

*/