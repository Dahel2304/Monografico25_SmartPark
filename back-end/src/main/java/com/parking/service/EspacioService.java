package com.parking.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parking.dto.AddEspaciosLoteDTO;
import com.parking.dto.EspacioResponseDTO;
import com.parking.dto.UpdateEspacioDTO;
import com.parking.dto.ReservaActivaDTO;
import com.parking.dto.TicketActivoDTO;
import com.parking.entity.Espacio;
import com.parking.entity.EstadoEspacio;
import com.parking.entity.Reserva;
import com.parking.entity.Ticket;
import com.parking.entity.TipoVehiculo;
import com.parking.repository.EspacioRepository;
import com.parking.repository.EstadoEspacioRepository;
import com.parking.repository.ReservaRepository;
import com.parking.repository.TicketRepository;
import com.parking.repository.TipoVehiculoRepository;

@Service
public class EspacioService {

    public static final int PISOS_TOTALES = 10;
    // private static final int TOTAL_CARROS = 198;
    // private static final int TOTAL_MOTOS = 8;
    private static final int CAPACIDAD_MOTO = 10;
    private static final String ESTADO_LIBRE = "libre";
    private static final String ESTADO_TICKET_ACTIVO = "activo";
    private static final String ESTADO_RESERVA_PENDIENTE = "pendiente";
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final EspacioRepository espacioRepository;
    private final TicketRepository ticketRepository;
    private final ReservaRepository reservaRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final EstadoEspacioRepository estadoEspacioRepository;

    public EspacioService(EspacioRepository espacioRepository, TicketRepository ticketRepository,
            ReservaRepository reservaRepository, TipoVehiculoRepository tipoVehiculoRepository,
            EstadoEspacioRepository estadoEspacioRepository) {
        this.espacioRepository = espacioRepository;
        this.ticketRepository = ticketRepository;
        this.reservaRepository = reservaRepository;
        this.tipoVehiculoRepository = tipoVehiculoRepository;
        this.estadoEspacioRepository = estadoEspacioRepository;
    }

    @Transactional
    public void migrarCodigosLegacy() {
        List<Espacio> espacios = espacioRepository.findAll();
        Set<String> codigosExistentes = new HashSet<>();

        for (Espacio espacio : espacios) {
            String codigoActual = normalizarCodigo(espacio.getCodigoEspacio());
            if (codigoActual.matches("[CM]-\\d+")) {
                String tipo = espacio.getTipoVehiculo() == null ? "CARRO" : espacio.getTipoVehiculo().getNombre();
                String prefijo = "CARRO".equalsIgnoreCase(tipo) ? "CP" : "MP";
                int numero = Integer.parseInt(codigoActual.substring(2));
                int piso = espacio.getPiso() == null ? 1 : espacio.getPiso();
                if (espacio.getCapacidad() == null) {
                    espacio.setCapacidad("MOTO".equalsIgnoreCase(tipo) ? CAPACIDAD_MOTO : 1);
                }
                String nuevoCodigo = generarCodigo(prefijo, piso, numero);
                if (!codigosExistentes.contains(nuevoCodigo)) {
                    espacio.setCodigoEspacio(nuevoCodigo);
                    espacio.setPiso(piso);
                    espacioRepository.save(espacio);
                    codigoActual = nuevoCodigo;
                }
            }
            if (espacio.getCapacidad() == null) {
                String tipo = espacio.getTipoVehiculo() == null ? "CARRO" : espacio.getTipoVehiculo().getNombre();
                espacio.setCapacidad("MOTO".equalsIgnoreCase(tipo) ? CAPACIDAD_MOTO : 1);
                espacioRepository.save(espacio);
            }
            codigosExistentes.add(codigoActual);
        }
    }

    @Transactional(readOnly = true)
    public List<EspacioResponseDTO> listarEspacios() {

        List<Espacio> espacios = espacioRepository.findAllByActivoTrueOrderByIdAsc();
        if (espacios.isEmpty()) {
            return List.of();
        }

        List<Long> espacioIds = espacios.stream().map(Espacio::getId).toList();
        Map<Long, Ticket> ticketActivoPorEspacio = obtenerTicketsActivosPorEspacio(espacioIds);
        Map<Long, Reserva> reservaPendientePorEspacio = obtenerReservasPendientesPorEspacio(espacioIds);

        List<EspacioResponseDTO> response = new ArrayList<>();

        for (Espacio espacio : espacios) {
            response.add(toDto(
                    espacio,
                    ticketActivoPorEspacio.get(espacio.getId()),
                    reservaPendientePorEspacio.get(espacio.getId())));
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<EspacioResponseDTO> listarEspaciosInactivos() {

        List<Espacio> espacios = espacioRepository.findAllByActivoFalseOrderByIdAsc();
        List<EspacioResponseDTO> response = new ArrayList<>();

        for (Espacio espacio : espacios) {
            response.add(toDto(espacio, null, null));
        }

        return response;
    }

    @Transactional
    public EspacioResponseDTO actualizarEstado(Long id, String nuevoEstado) {

        Espacio espacio = espacioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new NoSuchElementException("Espacio no encontrado"));

        EstadoEspacio estado = estadoEspacioRepository.findByNombreIgnoreCase(nuevoEstado.trim())
                .orElseThrow(() -> new NoSuchElementException("Estado de espacio no existe"));

        espacio.setEstado(estado);
        Espacio actualizado = espacioRepository.save(espacio);

        Optional<Ticket> ticketActivo = ticketRepository
                .findTopByEspacioIdAndEstadoNombreIgnoreCaseOrderByHoraEntradaDesc(actualizado.getId(), ESTADO_TICKET_ACTIVO);
        Optional<Reserva> reservaActiva = reservaRepository
            .findTopByEspacioIdAndEstadoNombreIgnoreCaseOrderByHoraInicioDesc(actualizado.getId(), ESTADO_RESERVA_PENDIENTE);

        return toDto(actualizado, ticketActivo.orElse(null), reservaActiva.orElse(null));
    }

    @Transactional
    public EspacioResponseDTO actualizarEspacio(Long id, UpdateEspacioDTO dto) {
        Espacio espacio = espacioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new NoSuchElementException("Espacio no encontrado o inactivo"));
        if (dto.getPiso() == null || dto.getPiso() < 1 || dto.getPiso() > PISOS_TOTALES) {
            throw new IllegalArgumentException("El piso debe estar entre 1 y " + PISOS_TOTALES);
        }
        String estadoActual = espacio.getEstado().getNombre();
        boolean ocupado = "OCUPADO".equalsIgnoreCase(estadoActual);
        if (ocupado && !espacio.getTipoVehiculo().getNombre().equalsIgnoreCase(dto.getTipoVehiculo())) {
            throw new IllegalStateException("No se puede cambiar el tipo de un espacio ocupado");
        }

        EstadoEspacio estado = estadoEspacioRepository.findByNombreIgnoreCase(dto.getEstado().trim())
                .orElseThrow(() -> new NoSuchElementException("Estado de espacio no existe"));
        TipoVehiculo tipo = tipoVehiculoRepository.findByNombreIgnoreCase(dto.getTipoVehiculo().trim())
                .orElseThrow(() -> new NoSuchElementException("Tipo de vehiculo no existe"));
        espacio.setCodigoEspacio(dto.getCodigoEspacio().trim().toUpperCase(Locale.ROOT));
        espacio.setTipoVehiculo(tipo);
        espacio.setEstado(estado);
        espacio.setPiso(dto.getPiso());
        espacio.setCapacidad("MOTO".equalsIgnoreCase(tipo.getNombre()) ? CAPACIDAD_MOTO : 1);
        return toDto(espacioRepository.save(espacio), null, null);
    }

    @Transactional
    public List<EspacioResponseDTO> agregarLote(AddEspaciosLoteDTO dto) {
        TipoVehiculo tipoCarro = tipoVehiculoRepository.findByNombreIgnoreCase("carro")
                .orElseThrow(() -> new NoSuchElementException("Tipo de vehiculo 'carro' no existe"));
        TipoVehiculo tipoMoto = tipoVehiculoRepository.findByNombreIgnoreCase("moto")
                .orElseThrow(() -> new NoSuchElementException("Tipo de vehiculo 'moto' no existe"));
        EstadoEspacio estadoLibre = estadoEspacioRepository.findByNombreIgnoreCase(ESTADO_LIBRE)
                .orElseThrow(() -> new NoSuchElementException("Estado 'libre' no existe"));

        List<Espacio> nuevos = new ArrayList<>();

        int[] motosPorPiso = { 3, 3, 2 };
        int[] carrosPorPiso = { 20, 20, 20, 20, 20, 20, 20, 20, 19, 19 };
        redistribuirEspaciosExistentes(tipoCarro, "CP", carrosPorPiso, 1);
        redistribuirEspaciosExistentes(tipoMoto, "MP", motosPorPiso, CAPACIDAD_MOTO);

        for (int piso = 1; piso <= PISOS_TOTALES; piso++) {
            agregarEspaciosFaltantes(nuevos, tipoCarro, estadoLibre, "CP", piso, carrosPorPiso[piso - 1], 1);
            if (piso <= 3) {
                agregarEspaciosFaltantes(nuevos, tipoMoto, estadoLibre, "MP", piso, motosPorPiso[piso - 1], CAPACIDAD_MOTO);
            }
        }

        List<Espacio> guardados = espacioRepository.saveAll(nuevos);
        List<EspacioResponseDTO> response = new ArrayList<>();

        for (Espacio espacio : guardados) {
            response.add(toDto(espacio, null, null));
        }

        return response;
    }

    @Transactional
    public void eliminarEspacio(Long id) {

        Espacio espacio = espacioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new NoSuchElementException("Espacio no encontrado"));

        if (!ESTADO_LIBRE.equalsIgnoreCase(espacio.getEstado().getNombre())) {
            throw new IllegalStateException("Solo se pueden eliminar espacios libres");
        }

        espacio.setActivo(false);
        espacioRepository.save(espacio);
    }

    @Transactional
    public void eliminarEspacioPermanente(Long id) {
        Espacio espacio = espacioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Espacio no encontrado"));

        if (ticketRepository.existsByEspacioId(espacio.getId())) {
            throw new IllegalStateException("No se puede eliminar el espacio porque tiene tickets asociados");
        }

        espacioRepository.delete(espacio);
    }

    @Transactional
    public EspacioResponseDTO reactivarEspacio(Long id) {

        Espacio espacio = espacioRepository.findByIdAndActivoFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Espacio inactivo no encontrado"));

        espacio.setActivo(true);
        Espacio reactivado = espacioRepository.save(espacio);

        return toDto(reactivado, null, null);
    }

    private Map<Long, Ticket> obtenerTicketsActivosPorEspacio(List<Long> espacioIds) {
        List<Ticket> ticketsActivos = ticketRepository
                .findAllByEspacioIdInAndEstadoNombreIgnoreCaseOrderByHoraEntradaDesc(espacioIds, ESTADO_TICKET_ACTIVO);

        Map<Long, Ticket> ticketPorEspacio = new HashMap<>();
        for (Ticket ticket : ticketsActivos) {
            Long espacioId = ticket.getEspacio() == null ? null : ticket.getEspacio().getId();
            if (espacioId != null && !ticketPorEspacio.containsKey(espacioId)) {
                ticketPorEspacio.put(espacioId, ticket);
            }
        }
        return ticketPorEspacio;
    }

    private Map<Long, Reserva> obtenerReservasPendientesPorEspacio(List<Long> espacioIds) {
        List<Reserva> reservasPendientes = reservaRepository
                .findAllByEspacioIdInAndEstadoNombreIgnoreCaseOrderByHoraInicioDesc(espacioIds, ESTADO_RESERVA_PENDIENTE);

        Map<Long, Reserva> reservaPorEspacio = new HashMap<>();
        for (Reserva reserva : reservasPendientes) {
            Long espacioId = reserva.getEspacio() == null ? null : reserva.getEspacio().getId();
            if (espacioId != null && !reservaPorEspacio.containsKey(espacioId)) {
                reservaPorEspacio.put(espacioId, reserva);
            }
        }
        return reservaPorEspacio;
    }

    private void agregarEspaciosFaltantes(List<Espacio> nuevos, TipoVehiculo tipoVehiculo, EstadoEspacio estadoLibre,
            String prefijo, int piso, int cantidadEsperada, int capacidad) {
        Set<Integer> numerosOcupados = obtenerNumerosOcupados(prefijo + piso);
        for (int numero = 1; numero <= cantidadEsperada; numero++) {
            if (!numerosOcupados.contains(numero)) {
                nuevos.add(crearEspacio(tipoVehiculo, estadoLibre, prefijo, numero, piso, capacidad));
            }
        }
    }

    private void redistribuirEspaciosExistentes(TipoVehiculo tipoVehiculo, String prefijo, int[] cantidadesPorPiso,
            int capacidad) {
        List<Espacio> existentes = espacioRepository.findAll().stream()
                .filter(espacio -> espacio.getTipoVehiculo() != null
                        && espacio.getTipoVehiculo().getId().equals(tipoVehiculo.getId()))
                .sorted(Comparator.comparing(Espacio::getId))
                .toList();

        int cantidadEsperada = 0;
        for (int cantidad : cantidadesPorPiso) {
            cantidadEsperada += cantidad;
        }

        List<Espacio> aRedistribuir = existentes.stream().limit(cantidadEsperada).toList();
        if (aRedistribuir.isEmpty()) {
            return;
        }

        for (Espacio espacio : aRedistribuir) {
            espacio.setCodigoEspacio("RECONFIG-" + prefijo + "-" + espacio.getId());
        }
        espacioRepository.saveAll(aRedistribuir);

        int indice = 0;
        for (int piso = 1; piso <= cantidadesPorPiso.length; piso++) {
            for (int numero = 1; numero <= cantidadesPorPiso[piso - 1] && indice < aRedistribuir.size(); numero++) {
                Espacio espacio = aRedistribuir.get(indice++);
                espacio.setCodigoEspacio(generarCodigo(prefijo, piso, numero));
                espacio.setPiso(piso);
                espacio.setCapacidad(capacidad);
            }
        }
        espacioRepository.saveAll(aRedistribuir);
    }

    private Espacio crearEspacio(TipoVehiculo tipoVehiculo, EstadoEspacio estadoLibre, String prefijo, int correlativo,
            Integer piso, int capacidad) {

        Espacio espacio = new Espacio();
        espacio.setCodigoEspacio(generarCodigo(prefijo, piso, correlativo));
        espacio.setPiso(piso);
        espacio.setCapacidad(capacidad);
        espacio.setTipoVehiculo(tipoVehiculo);
        espacio.setEstado(estadoLibre);
        espacio.setActivo(true);
        return espacio;
    }

    private Set<Integer> obtenerNumerosOcupados(String prefijo) {

        List<Espacio> existentes = espacioRepository.findByCodigoEspacioStartingWith(prefijo + "-");
        Pattern pattern = Pattern.compile("^" + prefijo + "-(\\d+)$");
        Set<Integer> ocupados = new HashSet<>();

        for (Espacio espacio : existentes) {
            String codigo = normalizarCodigo(espacio.getCodigoEspacio());
            Matcher matcher = pattern.matcher(codigo);
            if (matcher.matches()) {
                ocupados.add(Integer.parseInt(matcher.group(1)));
            }
        }

        return ocupados;
    }

    // private int obtenerPrimerNumeroDisponible(Set<Integer> ocupados) {
    //     int numero = 1;
    //     while (ocupados.contains(numero)) {
    //         numero++;
    //     }
    //     return numero;
    // }

    private String normalizarCodigo(String codigo) {
        if (codigo == null) {
            return "";
        }

        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String generarCodigo(String prefijo, int piso, int correlativo) {
        return String.format("%s%d-%03d", prefijo, piso, correlativo);
    }

    private EspacioResponseDTO toDto(Espacio espacio, Ticket ticketActivo, Reserva reservaActiva) {

        TicketActivoDTO ticketActivoDTO = null;
        if (ticketActivo != null && ticketActivo.getHoraEntrada() != null) {
            ticketActivoDTO = new TicketActivoDTO(
                    ticketActivo.getCodigoTicket(),
                    ticketActivo.getPlaca(),
                    ticketActivo.getHoraEntrada().format(HORA_FORMATTER),
                    ticketActivo.getHoraEntrada()
            );
        }

        ReservaActivaDTO reservaActivaDTO = null;
        if (reservaActiva != null && reservaActiva.getHoraInicio() != null) {
            reservaActivaDTO = new ReservaActivaDTO(
                reservaActiva.getCodigoReserva(),
                reservaActiva.getClienteNombreCompleto(),
                reservaActiva.getPlaca(),
                reservaActiva.getHoraInicio().format(HORA_FORMATTER)
            );
        }

        return new EspacioResponseDTO(
                espacio.getId(),
                espacio.getCodigoEspacio(),
                espacio.getPiso() == null ? 1 : espacio.getPiso(),
                espacio.getCapacidad() == null ? 1 : espacio.getCapacidad(),
                (int) ticketRepository.countByEspacioIdAndEstadoNombreIgnoreCase(espacio.getId(), "ACTIVO"),
                espacio.getTipoVehiculo().getNombre(),
                espacio.getEstado().getNombre(),
            ticketActivoDTO,
            reservaActivaDTO
        );
    }
}
