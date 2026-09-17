package com.parking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspacioResponseDTO {

    private Long id;
    private String codigoEspacio;
    private Integer piso;
    private Integer capacidad;
    private Integer ocupacionActual;
    private String tipoVehiculo;
    private String estado;
    private TicketActivoDTO ticketActivo;
    private ReservaActivaDTO reservaActiva;
}
