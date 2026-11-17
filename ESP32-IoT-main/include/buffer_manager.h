#ifndef BUFFER_MANAGER_H
#define BUFFER_MANAGER_H

#include "freertos/FreeRTOS.h"
#include "freertos/queue.h"

// Tipo do item do buffer
typedef int buffer_item_t;

// Handle do buffer (exportado)
extern QueueHandle_t sta_credenticial;
extern QueueHandle_t token_buffer;
extern QueueHandle_t reading_buffer;
// Inicializa buffer
void buffer_init(void);

#endif // BUFFER_MANAGER_H