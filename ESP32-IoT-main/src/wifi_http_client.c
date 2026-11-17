#include "wifi_http_client.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "esp_log.h"
#include "esp_http_client.h"
#include "esp_tls.h"
#include "miscellaneous.h"
#include "nvs_helper.h"
#include "buffer_manager.h"
#include "semaphore_manager.h"
#include "esp_crt_bundle.h"
#include "cJSON.h"
#include "esp_system.h"
#include "wifi.h"

static const char *TAG = "HTTP_CLIENT";
static esp_err_t http_event_handler(esp_http_client_event_t *evt)
{
   switch (evt->event_id) {
        case HTTP_EVENT_ON_DATA:
            // Verifica se o dado recebido é JSON e contém o token
            if (evt->data_len > 0) {
                // Exibir o JSON recebido no log
                ESP_LOGI("HTTP", "BODY len=%d: %.*s", evt->data_len, evt->data_len, (char*)evt->data);

                // Usando cJSON para parsear o JSON
                cJSON *json = cJSON_Parse((char*)evt->data);
                if (json == NULL) {
                    ESP_LOGE("HTTP", "Erro ao parsear JSON");
                    return ESP_FAIL;
                }

                // Buscar o item "sensor_token"
                cJSON *token = cJSON_GetObjectItem(json, "sensor_token");
                if (cJSON_IsString(token)) {
                    const char *sensor_token = token->valuestring;

                    // Chama a função para salvar o token
                    save_sensor_token(sensor_token);

                    ESP_LOGI("HTTP", "Token do sensor: %s", sensor_token);
                } else {
                    ESP_LOGE("HTTP", "sensor_token não encontrado ou não é uma string");
                }

                // Liberar o JSON após uso
                cJSON_Delete(json);
            }
            break;

        default:
            break;
    }

    return ESP_OK;
}

bool validate_sensor_token(const char *token)
{
    char payload[128];

    snprintf(payload, sizeof(payload),
        "{\"sensor_token\":\"%s\"}", token);

    ESP_LOGI("HTTP", "Payload: %s", payload);

    esp_http_client_config_t config = {
        .url = "https://daisy-projetc.onrender.com/validate_sensor",
        .method = HTTP_METHOD_POST,
        .timeout_ms = 5000,
        .transport_type = HTTP_TRANSPORT_OVER_SSL,
        .crt_bundle_attach = esp_crt_bundle_attach
        };

    esp_http_client_handle_t client = esp_http_client_init(&config);

    esp_http_client_set_header(client, "Content-Type", "application/json");
    esp_http_client_set_post_field(client, payload, strlen(payload));

    esp_err_t err = esp_http_client_perform(client);

    if (err != ESP_OK) {
        esp_http_client_cleanup(client);
        return false;
    }

    int status = esp_http_client_get_status_code(client);

    esp_http_client_cleanup(client);

    return status == 200;
}
bool register_sensor(char *token_out, size_t token_len)
{
    char mac[32];
    get_device_mac(mac);

    char payload[128];
    snprintf(payload, sizeof(payload), 
             "{\"mac\":\"%s\", \"token\":\"%s\"}", mac, token_out);

    ESP_LOGI("HTTP", "Payload: %s", payload);

    esp_http_client_config_t config = {
        .url = "https://daisy-projetc.onrender.com/register_sensor",
        .method = HTTP_METHOD_POST,
        .event_handler = http_event_handler,
        .timeout_ms = 30000,
        .transport_type = HTTP_TRANSPORT_OVER_SSL,
        .crt_bundle_attach = esp_crt_bundle_attach,
        .keep_alive_enable = false,
        .use_global_ca_store = true
    };

    esp_http_client_handle_t client = esp_http_client_init(&config);

    esp_http_client_set_header(client, "Content-Type", "application/json");
    esp_http_client_set_post_field(client, payload, strlen(payload));

    esp_err_t err = esp_http_client_perform(client);

    if (err == ESP_OK) {
        ESP_LOGI("HTTP", "Status = %d",
            esp_http_client_get_status_code(client));
    } else {
        ESP_LOGI("HTTP", "Request failed: %s", esp_err_to_name(err));
    }

    esp_http_client_cleanup(client);

    return err == ESP_OK;
}


void test_https_tls()
{
    ESP_LOGI("TLS_TEST", "Iniciando teste HTTPS com Google...");

    esp_http_client_config_t config = {
        .url = "https://www.google.com/",
        .method = HTTP_METHOD_GET,
        .timeout_ms = 10000,  // timeout de 10s
        .transport_type = HTTP_TRANSPORT_OVER_SSL,
        .crt_bundle_attach = esp_crt_bundle_attach,
        .use_global_ca_store = true
    };

    // Inicializa cliente
    esp_http_client_handle_t client = esp_http_client_init(&config);

    // Ativa logs detalhados de TLS
    esp_log_level_set("esp-tls", ESP_LOG_DEBUG);
    esp_log_level_set("HTTP_CLIENT", ESP_LOG_DEBUG);

    esp_err_t err = esp_http_client_perform(client);

    if (err == ESP_OK) {
        int status = esp_http_client_get_status_code(client);
        int length = esp_http_client_get_content_length(client);
        ESP_LOGI("TLS_TEST", "Conexão HTTPS OK!");
        ESP_LOGI("TLS_TEST", "Status HTTP: %d, Conteúdo: %d bytes", status, length);
    } else {
        ESP_LOGE("TLS_TEST", "Falha na requisição HTTPS: %s", esp_err_to_name(err));
    }

    esp_http_client_cleanup(client);
}

void sensor_provision()
{
    char token[64]= {0};;

    if (load_sensor_token(token, sizeof(token))) {

        ESP_LOGI("SENSOR", "Token encontrado, validando...");

        if (!validate_sensor_token(token)) {

            ESP_LOGE("SENSOR", "Token inválido");

            erase_wifi_credentials();
            erase_sensor_token();

            esp_restart();
        }

        ESP_LOGI("SENSOR", "Token válido");
        return;
    }

    ESP_LOGI("SENSOR", "Nenhum token encontrado, registrando novo sensor...");
    if (xQueueReceive(token_buffer, token, portMAX_DELAY)) {
        ESP_LOGI("SENSOR", "Token recebido do buffer: %s", token);
        test_https_tls();
        if (!register_sensor(token, sizeof(token))) {

            ESP_LOGE("SENSOR", "Falha ao registrar sensor");

            erase_wifi_credentials();
            erase_sensor_token();

            esp_restart();
        }
    }
    else{
        ESP_LOGE("SENSOR", "Não tem token de utilizador");

        erase_wifi_credentials();
        erase_sensor_token();

        esp_restart();
    }
    //save_sensor_token(token);

    ESP_LOGI("SENSOR", "Sensor registrado com token: %s", token);
}

static bool send_sensor_reading(const char *token)
{
    char payload[256];

    int humidity = 65;
    int light = 320;

    snprintf(payload, sizeof(payload),
        "{"
        "\"sensor_token\":\"%s\","
        "\"humidity\":%d,"
        "\"light\":%d"
        "}",
        token,
        humidity,
        light
    );

    ESP_LOGI("HTTP", "Payload: %s", payload);


    esp_http_client_config_t config = {
        .url = "https://daisy-projetc.onrender.com/sensor_reading",
        .method = HTTP_METHOD_POST,
        .timeout_ms = 5000,
        .crt_bundle_attach = esp_crt_bundle_attach
    };

    esp_http_client_handle_t client = esp_http_client_init(&config);

    esp_http_client_set_header(client, "Content-Type", "application/json");
    esp_http_client_set_post_field(client, payload, strlen(payload));

    esp_err_t err = esp_http_client_perform(client);

    if (err != ESP_OK) {
        ESP_LOGE(TAG, "HTTP error: %s", esp_err_to_name(err));
        esp_http_client_cleanup(client);
        return false;
    }

    int status = esp_http_client_get_status_code(client);

    ESP_LOGI(TAG, "HTTP status = %d", status);

    esp_http_client_cleanup(client);

    if (status == 200) {
        return true;
    }else if (status == 401) {
        ESP_LOGE(TAG, "Token inválido, apagando credenciais e reboot...");
        erase_wifi_credentials();
        erase_sensor_token();
        vTaskDelay(pdMS_TO_TICKS(1000));
        esp_restart();
    } else {
        ESP_LOGW(TAG, "Erro ao enviar leitura, status=%d, mas não reiniciando", status);
    }

    return false;
}


void wifi_http_client_task(void *pvParameters)
{
    char token[64];
    int readings= 0;
    // Espera WiFi STA conectar - troca semaphoro do gotip
    ESP_LOGI(TAG, "Aguardando WiFi...");
    xSemaphoreTake(gotip, portMAX_DELAY);
    xSemaphoreGive(gotip);
    vTaskDelay(pdMS_TO_TICKS(1000));
    

    sensor_provision();

    if (!load_sensor_token(token, sizeof(token))) {
        ESP_LOGE(TAG, "Token não encontrado após provision");
        esp_restart();
    }

    ESP_LOGI(TAG, "Token carregado: %s", token);

    while (1) {

        xSemaphoreTake(gotip, portMAX_DELAY);
        xSemaphoreGive(gotip);
        xSemaphoreGive(read);
        xQueueReceive(reading_buffer, &readings, pdMS_TO_TICKS(100));
        vTaskDelay(pdMS_TO_TICKS(1000)); 
        bool ok = send_sensor_reading(token);
        /* enviar a cada 10s */
        vTaskDelay(pdMS_TO_TICKS(10000));
    }
}
//busca o token load_sensor_token(char *token, size_t token_len)
    //caso tenha token validar token https://daisy-projetc.onrender.com/validate_sensor
        //caso error  apagar tudo (wifi_credental e sensor_token) e reboot
    //caso não tenha token registrar https://daisy-projetc.onrender.com/register_sensor
        //caso error  apagar tudo (wifi_credental e sensor_token) e reboot

//loop
    //registar leitura (valores estaticos) https://daisy-projetc.onrender.com/sensor_reading
        //caso erro em registar leitura de token apagar tudo (wifi_credental e sensor_token) e reboot