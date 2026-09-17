import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";

import { Button } from "@/components/ui/button";

export default function AddEspaciosDialog({ open, onClose, onSave }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async () => {
    setError("");

    try {
      setLoading(true);

      await onSave({ cantidadCarros: 198, cantidadMotos: 8, piso: 1 });

      onClose();
    } catch (err) {
      setError("Error al agregar espacios");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Configurar parqueo</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">

          <p className="text-sm text-muted-foreground">
            Se configuraran 10 pisos, 198 espacios para carros y 8 espacios para motos.
            Cada espacio de motos tendra capacidad para 10 motos y se distribuira en los pisos 1, 2 y 3.
          </p>

          {error && (
            <p className="text-sm text-red-500">
              {error}
            </p>
          )}

        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={onClose}
            disabled={loading}
          >
            Cancelar
          </Button>

          <Button
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading ? "Agregando..." : "Agregar"}
          </Button>
        </DialogFooter>

      </DialogContent>
    </Dialog>
  );
}