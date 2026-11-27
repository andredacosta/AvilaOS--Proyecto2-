# AvilaOS--Proyecto2-

Simulador de sistema operativo que integra ejecución coordinada del computador, planificación de disco, intercambio (*swap*), modelo de 7 estados de procesos y sistema de archivos.

## Módulos principales
- **Computador y E/S:** inicializa subsistemas, avanza el reloj (`t = t + 1`), actualiza procesos (ejecución, E/S y *deadlines*), despacha procesos listos a CPU y gestiona solicitudes `READ` / `WRITE` con bloqueo y actualización del PCB.

- **Planificación de disco:** encola y atiende peticiones `SolicitudDisco` sobre `IServicioDisco` / `TipoOperacionIO` mediante políticas `FIFO`, `SSTF`, `SCAN` y `C-SCAN`, moviendo el cabezal y notificando el fin de E/S.

- **Swap y 7 estados:** suspende procesos (`Swap Out` hacia `LISTO_SUSPENDIDO` o `BLOQUEADO_SUSPENDIDO`) y los reactiva (`Swap In` hacia `MemoriaRAM` y `LISTO`) coordinados con el PCB y el planificador de disco.

- **Sistema de archivos (`filesystem`):** implementa `NodoFS`, `Archivo`, `Directorio` y `SistemaArchivos` con `ListaEnlazada` propia (sin `java.util`), control de permisos (`ModoUsuario`, `PermisoArchivo`), operaciones CRUD/listado y gestión de bloques.
