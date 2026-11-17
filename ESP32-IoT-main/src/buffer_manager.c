#include "buffer_manager.h"
#include "esp_log.h"
#include "wifi.h"

#define BUFFER_LENGTH 10

QueueHandle_t sta_credenticial; // handle global
QueueHandle_t token_buffer; // handle global
QueueHandle_t reading_buffer; // handle global

void buffer_init(void)
{
    sta_credenticial = xQueueCreate(BUFFER_LENGTH, sizeof(wifi_sta_config_t));
    token_buffer = xQueueCreate(BUFFER_LENGTH, sizeof(char[64]));
    reading_buffer = xQueueCreate(1, sizeof(int));

    if (sta_credenticial == NULL)
    {
        ESP_LOGE("BUFFER", "Falha ao criar buffer");
    }
    if (token_buffer == NULL)
    {
        ESP_LOGE("BUFFER", "Falha ao criar buffer");
    }
    if (reading_buffer == NULL)
    {
        ESP_LOGE("BUFFER", "Falha ao criar buffer");
    }
}