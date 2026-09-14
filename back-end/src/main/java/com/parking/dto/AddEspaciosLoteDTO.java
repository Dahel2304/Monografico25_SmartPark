package com.parking.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AddEspaciosLoteDTO {

    @NotNull(message = "cantidadCarros es obligatoria")
    @Min(0)
    private Integer cantidadCarros;

    @NotNull(message = "cantidadMotos es obligatoria")
    @Min(0)
    private Integer cantidadMotos;

    @NotNull(message = "piso es obligatorio")
    @Positive(message = "El piso debe ser mayor que 0")
    private Integer piso = 1;

    @AssertTrue(message = "Debes agregar al menos un espacio")
    public boolean isCantidadTotalValida() {
        if (cantidadCarros == null || cantidadMotos == null) {
            return true;
        }
        return (cantidadCarros + cantidadMotos) > 0;
    }
}
