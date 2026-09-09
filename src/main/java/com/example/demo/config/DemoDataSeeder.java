package com.example.demo.config;

import com.example.demo.entity.Computador;
import com.example.demo.entity.PedidoReserva;
import com.example.demo.entity.ReservaComputador;
import com.example.demo.entity.ReservaSala;
import com.example.demo.entity.Sala;
import com.example.demo.entity.Usuario;
import com.example.demo.enums.NivelAcesso;
import com.example.demo.enums.StatusConta;
import com.example.demo.enums.StatusReserva;
import com.example.demo.enums.TipoPedido;
import com.example.demo.enums.TipoUsuario;
import com.example.demo.repository.ComputadorRepository;
import com.example.demo.repository.PedidoReservaRepository;
import com.example.demo.repository.ReservaComputadorRepository;
import com.example.demo.repository.ReservaSalaRepository;
import com.example.demo.repository.SalaRepository;
import com.example.demo.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Popula o banco com dados fictícios (usuários, reservas de sala e de
 * computador dos últimos ~70 dias úteis) só pra ter algo pra mostrar na
 * tela de Estatísticas. Roda uma única vez, e só se SEED_DEMO_DATA=true
 * estiver setado no ambiente e ainda não existir nenhuma reserva no banco.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UsuarioRepository usuarioRepo;
    private final SalaRepository salaRepo;
    private final ComputadorRepository computadorRepo;
    private final PedidoReservaRepository pedidoRepo;
    private final ReservaSalaRepository reservaSalaRepo;
    private final ReservaComputadorRepository reservaComputadorRepo;

    @Value("${SEED_DEMO_DATA:false}")
    private boolean seedDemoData;

    private final Random rand = new Random();

    public DemoDataSeeder(
            UsuarioRepository usuarioRepo,
            SalaRepository salaRepo,
            ComputadorRepository computadorRepo,
            PedidoReservaRepository pedidoRepo,
            ReservaSalaRepository reservaSalaRepo,
            ReservaComputadorRepository reservaComputadorRepo) {
        this.usuarioRepo = usuarioRepo;
        this.salaRepo = salaRepo;
        this.computadorRepo = computadorRepo;
        this.pedidoRepo = pedidoRepo;
        this.reservaSalaRepo = reservaSalaRepo;
        this.reservaComputadorRepo = reservaComputadorRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDemoData) return;

        if (reservaSalaRepo.count() > 0 || reservaComputadorRepo.count() > 0) {
            log.info("DemoDataSeeder: já existem reservas no banco, pulando seed.");
            return;
        }

        log.info("DemoDataSeeder: iniciando geração de dados fictícios...");

        List<Sala> salas = garantirSalas();
        List<Computador> computadores = garantirComputadores();
        List<Usuario> usuarios = garantirUsuariosFicticios();

        LocalTime[][] slots = {
                { LocalTime.of(8, 0), LocalTime.of(9, 30) },
                { LocalTime.of(10, 0), LocalTime.of(11, 30) },
                { LocalTime.of(13, 0), LocalTime.of(14, 30) },
                { LocalTime.of(15, 0), LocalTime.of(16, 30) },
                { LocalTime.of(17, 0), LocalTime.of(18, 30) },
                { LocalTime.of(19, 0), LocalTime.of(20, 30) },
        };

        int totalGerado = 0;
        LocalDate hoje = LocalDate.now();
        for (int diasAtras = 70; diasAtras >= 1; diasAtras--) {
            LocalDate dia = hoje.minusDays(diasAtras);
            if (dia.getDayOfWeek().getValue() > 5) continue; // só dias úteis

            for (Sala sala : salas) {
                totalGerado += gerarReservasDoDia(dia, slots, usuarios, sala, null);
            }
            for (Computador pc : computadores) {
                totalGerado += gerarReservasDoDia(dia, slots, usuarios, null, pc);
            }
        }

        log.info("DemoDataSeeder: {} reservas fictícias criadas com sucesso.", totalGerado);
    }

    private int gerarReservasDoDia(LocalDate dia, LocalTime[][] slots, List<Usuario> usuarios,
            Sala sala, Computador pc) {
        int qtdSlots = 1 + rand.nextInt(3); // 1 a 3 reservas nesse recurso nesse dia
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < slots.length; i++) indices.add(i);
        java.util.Collections.shuffle(indices, rand);

        int criadas = 0;
        for (int i = 0; i < Math.min(qtdSlots, slots.length); i++) {
            LocalTime[] slot = slots[indices.get(i)];
            LocalDateTime inicio = LocalDateTime.of(dia, slot[0]);
            LocalDateTime fim = LocalDateTime.of(dia, slot[1]);
            Usuario usuario = usuarios.get(rand.nextInt(usuarios.size()));
            StatusReserva status = statusAleatorio();

            criarPedidoEReserva(usuario, sala, pc, inicio, fim, status);
            criadas++;
        }
        return criadas;
    }

    private StatusReserva statusAleatorio() {
        int r = rand.nextInt(100);
        if (r < 70) return StatusReserva.FINALIZADA;
        if (r < 82) return StatusReserva.ATRASADO;
        if (r < 92) return StatusReserva.CANCELADA;
        return StatusReserva.REJEITADA;
    }

    private void criarPedidoEReserva(Usuario usuario, Sala sala, Computador pc,
            LocalDateTime inicio, LocalDateTime fim, StatusReserva status) {
        int qtdePessoas = sala != null ? 1 + rand.nextInt(Math.max(1, sala.getCapacidadePessoas())) : 1;

        PedidoReserva pedido = new PedidoReserva();
        pedido.setUsuario(usuario);
        pedido.setCriadaPorUsuario(usuario);
        pedido.setTipo(sala != null ? TipoPedido.SALA : TipoPedido.COMPUTADOR);
        pedido.setInicioPrevisto(inicio);
        pedido.setFimPrevisto(fim);
        pedido.setQtdePessoas(qtdePessoas);
        pedido.setStatus(status);
        pedido = pedidoRepo.save(pedido);

        LocalDateTime checkinEm = null, checkoutEm = null, canceladaEm = null, atrasadoEm = null;
        if (status == StatusReserva.FINALIZADA) {
            checkinEm = inicio.plusMinutes(rand.nextInt(9));
            checkoutEm = fim.plusMinutes(rand.nextInt(20) - 10);
            if (!checkoutEm.isAfter(checkinEm)) checkoutEm = checkinEm.plusMinutes(20);
        } else if (status == StatusReserva.ATRASADO) {
            atrasadoEm = inicio.plusMinutes(15);
        } else if (status == StatusReserva.CANCELADA) {
            canceladaEm = inicio.minusHours(1 + rand.nextInt(23));
        }

        if (sala != null) {
            ReservaSala r = new ReservaSala();
            r.setPedido(pedido);
            r.setSala(sala);
            r.setUsuario(usuario);
            r.setCriadaPorUsuario(usuario);
            r.setInicioPrevisto(inicio);
            r.setFimPrevisto(fim);
            r.setQtdePessoas(qtdePessoas);
            r.setStatus(status);
            r.setCheckinEm(checkinEm);
            r.setCheckoutEm(checkoutEm);
            r.setCanceladaEm(canceladaEm);
            r.setAtrasadoEm(atrasadoEm);
            reservaSalaRepo.save(r);
        } else {
            ReservaComputador r = new ReservaComputador();
            r.setPedido(pedido);
            r.setComputador(pc);
            r.setUsuario(usuario);
            r.setCriadaPorUsuario(usuario);
            r.setInicioPrevisto(inicio);
            r.setFimPrevisto(fim);
            r.setQtdePessoas(qtdePessoas);
            r.setStatus(status);
            r.setCheckinEm(checkinEm);
            r.setCheckoutEm(checkoutEm);
            r.setCanceladaEm(canceladaEm);
            r.setAtrasadoEm(atrasadoEm);
            reservaComputadorRepo.save(r);
        }
    }

    private List<Sala> garantirSalas() {
        List<Sala> existentes = salaRepo.findAll();
        if (!existentes.isEmpty()) return existentes;

        List<Sala> novas = new ArrayList<>();
        novas.add(criarSala("Sala de Estudos A", 4));
        novas.add(criarSala("Sala de Estudos B", 4));
        novas.add(criarSala("Sala de Reunião", 8));
        novas.add(criarSala("Sala Multimídia", 6));
        return novas;
    }

    private Sala criarSala(String nome, int capacidade) {
        Sala s = new Sala();
        s.setNome(nome);
        s.setCapacidadePessoas(capacidade);
        return salaRepo.save(s);
    }

    private List<Computador> garantirComputadores() {
        List<Computador> existentes = computadorRepo.findAll();
        if (!existentes.isEmpty()) return existentes;

        List<Computador> novos = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Computador c = new Computador();
            c.setCodigo(String.format("PC-%02d", i));
            c.setCapacidadePessoas(1);
            novos.add(computadorRepo.save(c));
        }
        return novos;
    }

    private List<Usuario> garantirUsuariosFicticios() {
        String[] nomes = {
                "Ana Beatriz Souza", "Bruno Almeida Silva", "Carla Mendes Rocha", "Diego Ferreira Lima",
                "Elisa Costa Ribeiro", "Fábio Nunes Cardoso", "Gabriela Pires Teixeira", "Henrique Barbosa Dias",
                "Isabela Martins Gomes", "João Vitor Araújo", "Karina Duarte Moura", "Lucas Correia Santana",
                "Mariana Rezende Alves", "Nathan Vieira Castro", "Otávio Ramos Fontoura", "Paula Cavalcante Farias",
        };
        TipoUsuario[] tipos = { TipoUsuario.SENAI, TipoUsuario.SESI, TipoUsuario.COLABORADOR, TipoUsuario.RESPONSAVEL };

        List<Usuario> todos = usuarioRepo.findAllIncludingInactive();
        List<Usuario> criados = new ArrayList<>();
        for (int i = 0; i < nomes.length; i++) {
            String cpf = String.format("999%08d", i);
            if (usuarioRepo.existsByCpf(cpf)) {
                todos.stream().filter(u -> cpf.equals(u.getCpf())).findFirst().ifPresent(criados::add);
                continue;
            }
            Usuario u = new Usuario();
            u.setNome(nomes[i]);
            u.setCpf(cpf);
            u.setEmail("demo.usuario" + (i + 1) + "@exemplo.com");
            u.setSenha(null);
            u.setTelefone("47999" + String.format("%06d", i));
            u.setNivelAcesso(NivelAcesso.PADRAO);
            u.setTipoUsuario(tipos[i % tipos.length]);
            u.setStatusConta(StatusConta.ATIVO);
            criados.add(usuarioRepo.save(u));
        }
        return criados;
    }
}
