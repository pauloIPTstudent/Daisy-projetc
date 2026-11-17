#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "nvs_flash.h"
#include "esp_err.h"
#include "wifi_provider.h"
#include "http_server_provider.h"
#include "buffer_manager.h"
#include "semaphore_manager.h"
#include "wifi_http_client.h"
#include "sensor_readings.h"
#include "cJSON.h"

static const char *TAG = "MAIN";



void app_main(void) {
    ESP_LOGI(TAG, "Inicializando sistema...");

    // Inicializa NVS
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    semaphore_init();
    buffer_init();
    wifi_provider_init();
    vTaskDelay(pdMS_TO_TICKS(1000)); 
    http_server_provider_init();

    xTaskCreate(
        wifi_provider_task,
        "wifi_provider",
        8192, //4096
        NULL,
        5,
        NULL
    );

    xTaskCreate(
        http_server_provider_task,
        "https_provider",
        8192,
        NULL,
        5,
        NULL
    );
    xTaskCreate(
        wifi_http_client_task,
        "https_client",
        12288,
        NULL,
        5,
        NULL
    );//*/

    xTaskCreate(
        sensor_readings_task,
        "sensor_readings",
        4096,
        NULL,
        5,
        NULL
    );
    while (1) {
        vTaskDelay(pdMS_TO_TICKS(5000)); 
    }
}