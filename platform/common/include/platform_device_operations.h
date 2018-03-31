#ifndef PLATFORM_DEVICE_OPERATIONS_H__
#define PLATFORM_DEVICE_OPERATIONS_H__

#include <stdlib.h>
#include "platform_types.h"
#include "platform_devctx.h"

typedef struct platform_device_operations {
	platform_res_t (*alloc)(platform_devctx_t *devctx,
			size_t const len,
			platform_mem_addr_t *addr,
			platform_alloc_flags_t const flags);
	platform_res_t (*dealloc)(platform_devctx_t *devctx,
			platform_mem_addr_t const addr,
			platform_alloc_flags_t const flags);
	platform_res_t (*read_mem)(platform_devctx_t *devctx,
			platform_mem_addr_t const addr,
			size_t const length,
			void *data,
			platform_mem_flags_t const flags);
	platform_res_t (*write_mem)(platform_devctx_t *devctx,
			platform_mem_addr_t const addr,
			size_t const length,
			void const *data,
			platform_mem_flags_t const flags);
	platform_res_t (*read_ctl)(platform_devctx_t *devctx,
			platform_ctl_addr_t const addr,
			size_t const length,
			void *data,
			platform_ctl_flags_t const flags);
	platform_res_t (*write_ctl)(platform_devctx_t *devctx,
			platform_ctl_addr_t const addr,
			size_t const length,
			void const *data,
			platform_ctl_flags_t const flags);
} platform_device_operations_t;

#endif /* PLATFORM_DEVICE_OPERATIONS_H__ */