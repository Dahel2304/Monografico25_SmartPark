package com.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateEspacioDTO {

    @NotBlank(message = "El codigo del espacio es obligatorio")
    private String codigoEspacio;

    @NotBlank(message = "El tipo de vehiculo es obligatorio")
    private String tipoVehiculo;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotNull(message = "El piso es obligatorio")
    @Positive(message = "El piso debe ser mayor que 0")
    private Integer piso;
}