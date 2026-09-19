import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";

import {
  crearReserva
} from "../../api/reservas";

import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Badge } from "../ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { cn } from "@/lib/utils";
import { getEspacios } from "@/api/espacios";

export default function CrearReserva({ onSuccess }) {

  const [placa, setPlaca] = useState("");
  const [tipoVehiculo, setTipoVehiculo] = useState("CARRO");
  const [fechaReserva, setFechaReserva] = useState("");

  const [espacioId, setEspacioId] = useState("");
  const [pisoSeleccionado, setPisoSeleccionado] = useState("");
  const [espacios, setEspacios] = useState([]);
  const [reservaCreada, setReservaCreada] = useState(null);

  const [nombre, setNombre] = useState("");
  const [apellido, setApellido] = useState("");
  const [horaInicio, setHoraInicio] = useState("");
  const [email, setEmail] = useState("");
  const [telefono, setTelefono] = useState("");

  const [loading, setLoading] = useState(false);

  const formatDateTime = (value) => {
    if (!value) {
      return "-";
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat("es-DO", {
      dateStyle: "short",
      timeStyle: "short"
    }).format(date);
  };

  const getMinHoraInicio = () => {
    if (!fechaReserva) return "";
    const today = new Date();
    const todayIso = today.toLocaleDateString("en-CA");
    if (fechaReserva !== todayIso) return "";

    const hours = String(today.getHours()).padStart(2, "0");
    const minutes = String(today.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  };

  const fetchEspacios = async () => {
    try {
      const data = await getEspacios();
      setEspacios(data);
    } catch (err) {
      console.error(err);
      toast.error("Error cargando espacios disponibles");
    }
  };

  useEffect(() => {
    fetchEspacios();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);

      const fechaHoraInicio = `${fechaReserva}T${horaInicio}:00`;

      const espacioSeleccionado = espacios.find(
        (espacio) => espacio.id === Number(espacioId)
      );

      if (!espacioSeleccionado || String(espacioSeleccionado.estado).toUpperCase() !== "LIBRE") {
        throw new Error("Debe seleccionar un espacio valido");
      }

      const data = {
        espacioId: espacioSeleccionado.id,
        placa,
        tipoVehiculo: String(espacioSeleccionado.tipoVehiculo).toUpperCase(),
        horaInicio: fechaHoraInicio,
        clienteNombreCompleto: `${nombre} ${apellido}`.trim(),
        clienteTelefono: telefono,
        clienteEmail: email
      };

      const reservaCreadaResponse = await crearReserva(data);

      setReservaCreada({
        codigoReserva: reservaCreadaResponse.codigoReserva,
        nombre: reservaCreadaResponse.clienteNombreCompleto,
        email: reservaCreadaResponse.clienteEmail,
        telefono: reservaCreadaResponse.clienteTelefono,
        placa: reservaCreadaResponse.placa,
        tipoVehiculo: reservaCreadaResponse.tipoVehiculo,
        horaInicio: reservaCreadaResponse.horaInicio,
        espacio: reservaCreadaResponse.codigoEspacio,
        correoEnviado: reservaCreadaResponse.correoEnviado
      });

      if (!reservaCreadaResponse.correoEnviado) {
        toast.error("Reserva creada, pero no se pudo enviar el correo");
      } else {
        toast.success("Reserva creada correctamente");
      }

      setPlaca("");
      setFechaReserva("");
      setHoraInicio("");
      setNombre("");
      setApellido("");
      setEmail("");
      setTelefono("");
      setEspacioId("");

      await fetchEspacios();

      if (onSuccess) {
        onSuccess();
      }
    } catch (err) {
      const mensajeError =
        err?.response?.data?.message ||
        err.message ||
        "Error creando reserva";

      console.error("Error creando reserva:", err?.response?.data || err);
      toast.error(mensajeError);
    } finally {
      setLoading(false);
    }
  };

  const espaciosLibres = espacios.filter(
    (espacio) => String(espacio.estado || "").toUpperCase() === "LIBRE"
  );

  const carrosDisponibles = espaciosLibres.filter(
    (espacio) => espacio.tipoVehiculo === "CARRO"
  ).length;

  const motosDisponibles = espaciosLibres.filter(
    (espacio) => espacio.tipoVehiculo === "MOTO"
  ).length;

  const espaciosFiltrados = espacios.filter(
    (espacio) => String(espacio.tipoVehiculo || "").toUpperCase() === tipoVehiculo
      && ["LIBRE", "MANTENIMIENTO"].includes(String(espacio.estado || "").toUpperCase())
  );

  const pisosDisponibles = useMemo(
    () => [...new Set(espaciosFiltrados.map((espacio) => Number(espacio.piso || 1)))].sort((a, b) => a - b),
    [espaciosFiltrados]
  );

  const espaciosDelPiso = espaciosFiltrados.filter(
    (espacio) => String(espacio.piso || 1) === String(pisoSeleccionado)
  );

  useEffect(() => {
    if (!pisosDisponibles.some((piso) => String(piso) === String(pisoSeleccionado))) {
      setPisoSeleccionado(pisosDisponibles.length ? String(pisosDisponibles[0]) : "");
    }
  }, [pisosDisponibles, pisoSeleccionado]);

  useEffect(() => {
    const espacioSeleccionado = espacios.find((espacio) => String(espacio.id) === String(espacioId));
    if (!espacioSeleccionado || String(espacioSeleccionado.tipoVehiculo || "").toUpperCase() !== tipoVehiculo) {
      setEspacioId("");
    }
  }, [espacios, espacioId, tipoVehiculo]);

  return (
    <div className="space-y-6">
      {reservaCreada && (
        <Card className="border-emerald-300 bg-emerald-50">
          <CardHeader className="pb-2">
            <CardTitle className="text-emerald-700 text-base">
              Reserva creada
            </CardTitle>
          </CardHeader>

          <CardContent className="grid gap-2 text-sm md:grid-cols-2">
            <div><strong>Codigo:</strong> {reservaCreada.codigoReserva}</div>
            <div><strong>Espacio:</strong> {reservaCreada.espacio}</div>
            <div><strong>Cliente:</strong> {reservaCreada.nombre}</div>
            <div><strong>Correo:</strong> {reservaCreada.email}</div>
            <div><strong>Placa:</strong> {reservaCreada.placa}</div>
            <div><strong>Inicio:</strong> {formatDateTime(reservaCreada.horaInicio)}</div>
            <div className="md:col-span-2 flex justify-end">
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  navigator.clipboard.writeText(reservaCreada.codigoReserva);
                  toast.success("Codigo de reserva copiado");
                }}
              >
                Copiar codigo
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="flex flex-wrap items-center gap-2 rounded-md border bg-card px-3 py-2">
        <span className="text-xs font-medium text-muted-foreground">
          Disponibilidad:
        </span>
        <Badge variant="secondary" className="gap-1">
          Carros <span className="font-bold">{carrosDisponibles}</span>
        </Badge>
        <Badge variant="secondary" className="gap-1">
          Motos <span className="font-bold">{motosDisponibles}</span>
        </Badge>
        <Badge variant="outline" className="gap-1">
          Total libres <span className="font-bold">{espaciosLibres.length}</span>
        </Badge>
      </div>

      <form
        onSubmit={handleSubmit}
        className="space-y-4"
      >
            <div className="rounded-md border bg-white p-3 shadow-sm space-y-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Datos del cliente
              </p>
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <Label>Nombre</Label>
                  <Input
                    value={nombre}
                    onChange={(e) => setNombre(e.target.value)}
                    placeholder="Nombre"
                    required
                  />
                </div>

                <div>
                  <Label>Apellido</Label>
                  <Input
                    value={apellido}
                    onChange={(e) => setApellido(e.target.value)}
                    placeholder="Apellido"
                    required
                  />
                </div>

                <div>
                  <Label>Email</Label>
                  <Input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="cliente@email.com"
                    required
                  />
                </div>

                <div>
                  <Label>Telefono</Label>
                  <Input
                    type="tel"
                    value={telefono}
                    onChange={(e) => setTelefono(e.target.value)}
                    placeholder="809-555-1234"
                    required
                  />
                </div>
              </div>
            </div>

            <div className="rounded-md border bg-white p-3 shadow-sm space-y-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Vehiculo y parqueo
              </p>
              <div className="space-y-2">
                <Label>Tipo de Vehiculo</Label>
                <div className="flex gap-2">
                  <label
                    className={cn(
                      "flex items-center gap-2 rounded-md border px-3 py-2 text-sm cursor-pointer transition-colors",
                      tipoVehiculo === "CARRO"
                        ? "border-primary bg-primary text-primary-foreground"
                        : "border-border bg-background hover:bg-muted"
                    )}
                  >
                    <input
                      type="radio"
                      className="sr-only"
                      value="CARRO"
                      checked={tipoVehiculo === "CARRO"}
                      onChange={(e) => {
                        setTipoVehiculo(e.target.value);
                        setEspacioId("");
                      }}
                    />
                    Carro
                  </label>

                  <label
                    className={cn(
                      "flex items-center gap-2 rounded-md border px-3 py-2 text-sm cursor-pointer transition-colors",
                      tipoVehiculo === "MOTO"
                        ? "border-primary bg-primary text-primary-foreground"
                        : "border-border bg-background hover:bg-muted"
                    )}
                  >
                    <input
                      type="radio"
                      className="sr-only"
                      value="MOTO"
                      checked={tipoVehiculo === "MOTO"}
                      onChange={(e) => {
                        setTipoVehiculo(e.target.value);
                        setEspacioId("");
                      }}
                    />
                    Moto
                  </label>
                </div>
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <Label>Placa del Vehículo</Label>
                  <Input
                    value={placa}
                    onChange={(e) => setPlaca(e.target.value.toUpperCase())}
                    placeholder="A123456"
                    required
                  />
                </div>

                <div className="space-y-3 md:col-span-2">
                  <div className="flex items-center justify-between gap-2">
                    <Label>Seleccionar parqueo por piso</Label>
                    <span className="text-xs text-muted-foreground">
                      {espaciosLibres.filter((espacio) => String(espacio.tipoVehiculo || "").toUpperCase() === tipoVehiculo).length} libres
                    </span>
                  </div>

                  <div className="flex flex-wrap gap-2" role="tablist" aria-label="Pisos disponibles">
                    {pisosDisponibles.map((piso) => (
                      <button
                        key={piso}
                        type="button"
                        role="tab"
                        aria-selected={String(piso) === String(pisoSeleccionado)}
                        onClick={() => setPisoSeleccionado(String(piso))}
                        className={cn(
                          "rounded-md border px-3 py-2 text-sm font-medium transition-colors",
                          String(piso) === String(pisoSeleccionado)
                            ? "border-primary bg-primary text-primary-foreground"
                            : "border-border bg-background hover:bg-muted"
                        )}
                      >
                        Piso {piso}
                      </button>
                    ))}
                  </div>

                  <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-6">
                    {espaciosDelPiso.map((espacio) => {
                      const enMantenimiento = String(espacio.estado || "").toUpperCase() === "MANTENIMIENTO";
                      const seleccionado = String(espacio.id) === String(espacioId);
                      return (
                        <button
                          key={espacio.id}
                          type="button"
                          disabled={enMantenimiento}
                          onClick={() => setEspacioId(String(espacio.id))}
                          className={cn(
                            "min-h-16 rounded-md border px-2 py-2 text-left text-sm transition-colors",
                            enMantenimiento
                              ? "cursor-not-allowed border-amber-300 bg-amber-50 text-amber-800"
                              : seleccionado
                                ? "border-primary bg-primary text-primary-foreground ring-2 ring-primary/30"
                                : "border-emerald-300 bg-emerald-50 text-emerald-900 hover:bg-emerald-100"
                          )}
                        >
                          <span className="block font-semibold">{espacio.codigoEspacio}</span>
                          <span className="block text-xs">
                            {enMantenimiento ? "En mantenimiento" : "Libre"}
                          </span>
                        </button>
                      );
                    })}
                  </div>

                  {!espaciosDelPiso.length && (
                    <p className="rounded-md border border-dashed p-3 text-xs text-muted-foreground">
                      No hay espacios para este tipo de vehículo en el piso seleccionado.
                    </p>
                  )}
                  {espaciosFiltrados.some((espacio) => String(espacio.estado || "").toUpperCase() === "MANTENIMIENTO") && (
                    <p className="text-xs font-medium text-amber-700">
                      Los espacios en mantenimiento se muestran bloqueados y no se pueden seleccionar.
                    </p>
                  )}
                  <input type="hidden" value={espacioId} required aria-label="Parqueo seleccionado" />
                </div>
              </div>
            </div>

            <div className="rounded-md border bg-white p-3 shadow-sm space-y-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Programacion
              </p>
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <Label>Fecha Reserva</Label>
                  <Input
                    type="date"
                    value={fechaReserva}
                    onChange={(e) => setFechaReserva(e.target.value)}
                    min={new Date().toLocaleDateString("en-CA")}
                    required
                  />
                </div>

                <div>
                  <Label>Hora Inicio</Label>
                  <Input
                    type="time"
                    value={horaInicio}
                    onChange={(e) => setHoraInicio(e.target.value)}
                    min={getMinHoraInicio()}
                    required
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end">
              <Button
                className="min-w-40 bg-primary text-primary-foreground hover:bg-primary/90"
                disabled={loading || espaciosFiltrados.length === 0}
              >
                {loading ? "Creando..." : "Crear Reserva"}
              </Button>
            </div>

      </form>

    </div>

  );

}