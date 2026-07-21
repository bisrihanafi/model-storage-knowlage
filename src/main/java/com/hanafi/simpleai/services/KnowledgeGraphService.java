/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hanafi.simpleai.services;

import com.hanafi.simpleai.exception.KnowledgeGraphException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 
 * Core engine dari Digital Neuron AI.
 *
 * Kelas ini SENGAJA tidak punya dependensi ke Scanner atau System.out - semua
 * method menerima parameter biasa dan mengembalikan objek Java biasa (List,
 * Neuron, Edge, dst), atau melempar KnowledgeGraphException kalau ada error.
 *
 * <pre>
 * Dengan begitu kelas ini bisa dipakai persis sama oleh:
 *   - ConsoleApp (aplikasi menu di terminal)
 *   - REST API controller (nanti, mis. Spring Boot @RestController)
 *   - kode Java lain mana pun yang butuh fungsi knowledge graph ini
 *
 * Data disimpan di dua file:
 *   - neuronsFilePath : fakta dasar (NEURON / LINK), format sama seperti sebelumnya
 *   - rulesFilePath   : aturan komposisi/relasi turunan (CHAIN / TRANSITIVE)
 * </pre>
 */
public class KnowledgeGraphService {

    private static final double DEFAULT_WEIGHT = 1.0;
    private static final double PENGUATAN = 0.1;
    private static final String EVENT_PREFIX = "event_";
    private static final int MAX_KEDALAMAN_ATURAN = 10;

    private final Map<String, Neuron> neurons = new LinkedHashMap<>();
    private final Map<String, Aturan> aturanMap = new LinkedHashMap<>(); // key = nama aturan (lowercase)

    private final String neuronsFilePath;
    private final String rulesFilePath;

    public KnowledgeGraphService() {
        this("neurons.txt", "rules.txt");
    }

    public KnowledgeGraphService(String neuronsFilePath, String rulesFilePath) {
        this.neuronsFilePath = neuronsFilePath;
        this.rulesFilePath = rulesFilePath;
    }

    // =====================================================================
    // DATA TRANSFER OBJECTS (record) - dipakai sebagai bentuk hasil query
    // =====================================================================

    /** Satu fakta lengkap (Subjek, Predikat, Objek, Bobot), dipakai untuk menampilkan seluruh jaringan. */
    public record Fakta(String subjekId, String predikat, String objekId, double bobot) {
    }

    /** Satu langkah hasil penjelajahan (aktivasi) neuron. */
    public record LangkahAktivasi(String asalId, String predikat, String tujuanId, int kedalaman) {
    }

    // =====================================================================
    // LIFECYCLE: load & save
    // =====================================================================

    /** Memuat data neuron & fakta dari file. Aman dipanggil walau file belum ada. */
    public void muatData() {
        File file = new File(neuronsFilePath);
        if (!file.exists()) return;

        List<String[]> pendingLinks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", 5);

                if (parts[0].equals("NEURON") && parts.length >= 3) {
                    neurons.put(parts[1], new Neuron(parts[1], parts[2]));
                } else if (parts[0].equals("LINK") && parts.length == 5) {
                    pendingLinks.add(parts);
                }
            }
        } catch (IOException e) {
            throw new KnowledgeGraphException("Gagal membaca " + neuronsFilePath + ": " + e.getMessage());
        }

        for (String[] parts : pendingLinks) {
            String subjek = parts[1];
            String predikat = parts[2];
            String objek = parts[3];
            double bobot = parseDoubleAman(parts[4], DEFAULT_WEIGHT);

            if (neurons.containsKey(subjek) && neurons.containsKey(objek)) {
                neurons.get(subjek).tambahRelasiKeluar(new Edge(objek, predikat, bobot));
            }
        }
    }

    /**
     * Memuat aturan komposisi dari rules.txt. Format per baris:
     *   CHAIN|namaAturan|predikat1|predikat2|excludeSelf(true/false)
     *   TRANSITIVE|namaAturan|predikat
     * Baris kosong atau diawali '#' diabaikan (komentar). Aman kalau file belum ada.
     */
    public void muatAturan() {
        File file = new File(rulesFilePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|");
                if (parts[0].equalsIgnoreCase("CHAIN") && parts.length >= 4) {
                    boolean excludeSelf = parts.length >= 5 && parts[4].equalsIgnoreCase("true");
                    Aturan a = new Aturan(parts[1], Aturan.Tipe.CHAIN, parts[2], parts[3], excludeSelf);
                    aturanMap.put(a.getNama().toLowerCase(), a);
                } else if (parts[0].equalsIgnoreCase("TRANSITIVE") && parts.length >= 3) {
                    Aturan a = new Aturan(parts[1], Aturan.Tipe.TRANSITIVE, parts[2], null, false);
                    aturanMap.put(a.getNama().toLowerCase(), a);
                }
            }
        } catch (IOException e) {
            throw new KnowledgeGraphException("Gagal membaca " + rulesFilePath + ": " + e.getMessage());
        }
    }

    /** Menyimpan seluruh neuron & fakta ke file (dipanggil otomatis tiap ada perubahan data). */
    private void simpanData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(neuronsFilePath))) {
            for (Neuron n : neurons.values()) {
                writer.write("NEURON|" + n.getId() + "|" + n.getContent());
                writer.newLine();
            }
            for (Neuron n : neurons.values()) {
                for (Edge e : n.getOutgoing()) {
                    writer.write("LINK|" + n.getId() + "|" + e.getLabel() + "|" + e.getTargetId() + "|" + e.getWeight());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new KnowledgeGraphException("Gagal menyimpan ke " + neuronsFilePath + ": " + e.getMessage());
        }
    }

    private double parseDoubleAman(String text, double fallback) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // =====================================================================
    // CRUD NEURON
    // =====================================================================

    public Neuron tambahNeuron(String id, String content) {
        if (id == null || id.isBlank()) {
            throw new KnowledgeGraphException("ID neuron tidak boleh kosong.");
        }
        if (neurons.containsKey(id)) {
            throw new KnowledgeGraphException("Neuron dengan ID '" + id + "' sudah ada.");
        }
        Neuron neuron = new Neuron(id, content == null ? "" : content);
        neurons.put(id, neuron);
        simpanData();
        return neuron;
    }

    /** Mengambil satu neuron berdasarkan ID. Melempar exception kalau tidak ditemukan. */
    public Neuron getNeuron(String id) {
        Neuron n = neurons.get(id);
        if (n == null) {
            throw new KnowledgeGraphException("Neuron '" + id + "' tidak ditemukan.");
        }
        return n;
    }

    public boolean adaNeuron(String id) {
        return neurons.containsKey(id);
    }

    public List<Neuron> getSemuaNeuron() {
        return new ArrayList<>(neurons.values());
    }

    public void hapusNeuron(String id) {
        if (neurons.remove(id) == null) {
            throw new KnowledgeGraphException("Neuron '" + id + "' tidak ditemukan.");
        }
        for (Neuron n : neurons.values()) {
            n.hapusRelasiKeKe(id);
        }
        simpanData();
    }

    // =====================================================================
    // RELASI DASAR (TRIPLE)
    // =====================================================================

    public Edge tambahRelasi(String subjekId, String predikat, String objekId, Double bobot) {
        Neuron subjek = getNeuron(subjekId); // otomatis lempar exception kalau tidak ada
        getNeuron(objekId);

        if (predikat == null || predikat.isBlank()) {
            throw new KnowledgeGraphException("Predikat tidak boleh kosong.");
        }

        Edge edge = new Edge(objekId, predikat, bobot != null ? bobot : DEFAULT_WEIGHT);
        subjek.tambahRelasiKeluar(edge);
        simpanData();
        return edge;
    }

    /** Semua fakta (Subjek, Predikat, Objek, Bobot) di seluruh jaringan. */
    public List<Fakta> getSemuaFakta() {
        List<Fakta> hasil = new ArrayList<>();
        for (Neuron n : neurons.values()) {
            for (Edge e : n.getOutgoing()) {
                hasil.add(new Fakta(n.getId(), e.getLabel(), e.getTargetId(), e.getWeight()));
            }
        }
        return hasil;
    }

    // =====================================================================
    // FAKTA MAJEMUK (REIFIKASI)
    // =====================================================================

    /**
     * Membuat neuron "peristiwa" baru dan menghubungkannya dari subjek.
     * Dipakai saat satu fakta butuh banyak detail sekaligus (mis. riwayat kerja).
     *
     * @param eventId ID kustom untuk peristiwa, atau null/kosong untuk auto-generate.
     * @return ID peristiwa yang dipakai (baik kustom maupun hasil auto-generate).
     */
    public String buatPeristiwa(String subjekId, String predikatUtama, String eventId, String deskripsi) {
        getNeuron(subjekId);

        if (predikatUtama == null || predikatUtama.isBlank()) {
            throw new KnowledgeGraphException("Predikat utama tidak boleh kosong.");
        }

        String idPeristiwa = (eventId == null || eventId.isBlank())
                ? EVENT_PREFIX + hitungEventBerikutnya()
                : eventId;

        if (neurons.containsKey(idPeristiwa)) {
            throw new KnowledgeGraphException("ID peristiwa '" + idPeristiwa + "' sudah dipakai.");
        }

        neurons.put(idPeristiwa, new Neuron(idPeristiwa, deskripsi == null ? "" : deskripsi));
        neurons.get(subjekId).tambahRelasiKeluar(new Edge(idPeristiwa, predikatUtama, DEFAULT_WEIGHT));
        simpanData();
        return idPeristiwa;
    }

    /**
     * Menambahkan satu detail ke peristiwa yang sudah dibuat lewat buatPeristiwa().
     * Kalau neuron objek belum ada, akan dibuat otomatis dengan deskripsiObjekBaru.
     */
    public void tambahDetailPeristiwa(String eventId, String predikat, String objekId, String deskripsiObjekBaru) {
        Neuron event = getNeuron(eventId);

        if (predikat == null || predikat.isBlank()) {
            throw new KnowledgeGraphException("Predikat detail tidak boleh kosong.");
        }
        if (objekId == null || objekId.isBlank()) {
            throw new KnowledgeGraphException("Objek detail tidak boleh kosong.");
        }

        if (!neurons.containsKey(objekId)) {
            neurons.put(objekId, new Neuron(objekId, deskripsiObjekBaru == null ? "" : deskripsiObjekBaru));
        }

        event.tambahRelasiKeluar(new Edge(objekId, predikat, DEFAULT_WEIGHT));
        simpanData();
    }

    private int hitungEventBerikutnya() {
        int max = 0;
        for (String id : neurons.keySet()) {
            if (id.startsWith(EVENT_PREFIX)) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(EVENT_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                    // ID event dengan format tak biasa, lewati
                }
            }
        }
        return max + 1;
    }

    // =====================================================================
    // QUERY DASAR
    // =====================================================================

    /** Diketahui SUBJEK + PREDIKAT, cari OBJEK (beserta detailnya jika neuron perantara/event). */
    public List<Neuron> cariObjek(String subjekId, String predikat) {
        Neuron subjek = getNeuron(subjekId);
        List<Neuron> hasil = new ArrayList<>();
        for (Edge e : subjek.getOutgoing()) {
            if (e.getLabel().equalsIgnoreCase(predikat)) {
                Neuron objek = neurons.get(e.getTargetId());
                if (objek != null) hasil.add(objek);
            }
        }
        return hasil;
    }

    /** Diketahui PREDIKAT + OBJEK, cari SUBJEK. */
    public List<Neuron> cariSubjek(String predikat, String objekId) {
        List<Neuron> hasil = new ArrayList<>();
        for (Neuron n : neurons.values()) {
            for (Edge e : n.getOutgoing()) {
                if (e.getLabel().equalsIgnoreCase(predikat) && e.getTargetId().equalsIgnoreCase(objekId)) {
                    hasil.add(n);
                    break;
                }
            }
        }
        return hasil;
    }

    /**
     * Menjelajah (BFS) semua relasi keluar dari satu neuron sampai kedalaman tertentu,
     * lalu memperkuat bobot tiap relasi yang dilalui (efek Hebbian).
     */
    public List<LangkahAktivasi> aktivasiNeuron(String startId) {
        return aktivasiNeuron(startId, 3);
    }

    public List<LangkahAktivasi> aktivasiNeuron(String startId, int maxDepth) {
        Neuron start = getNeuron(startId);

        List<LangkahAktivasi> hasil = new ArrayList<>();
        List<Edge> dilewati = new ArrayList<>();

        Set<String> visited = new LinkedHashSet<>();
        visited.add(start.getId());

        Deque<String> queue = new ArrayDeque<>();
        Map<String, Integer> depthMap = new LinkedHashMap<>();
        queue.add(start.getId());
        depthMap.put(start.getId(), 0);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            int depth = depthMap.get(currentId);
            if (depth >= maxDepth) continue;

            Neuron current = neurons.get(currentId);
            List<Edge> edges = new ArrayList<>(current.getOutgoing());
            edges.sort((e1, e2) -> Double.compare(e2.getWeight(), e1.getWeight()));

            for (Edge edge : edges) {
                if (!neurons.containsKey(edge.getTargetId())) continue;

                hasil.add(new LangkahAktivasi(currentId, edge.getLabel(), edge.getTargetId(), depth + 1));
                dilewati.add(edge);

                if (!visited.contains(edge.getTargetId())) {
                    visited.add(edge.getTargetId());
                    depthMap.put(edge.getTargetId(), depth + 1);
                    queue.add(edge.getTargetId());
                }
            }
        }

        if (!dilewati.isEmpty()) {
            for (Edge e : dilewati) {
                e.perkuatBobot(PENGUATAN);
            }
            simpanData();
        }

        return hasil;
    }

    // =====================================================================
    // RULE ENGINE (RELASI TURUNAN / PREDICATE COMPOSITION)
    // =====================================================================

    public List<Aturan> getSemuaAturan() {
        return new ArrayList<>(aturanMap.values());
    }

    /** Semua predikat unik yang pernah dipakai di data, dipakai sebagai referensi saat membuat aturan baru. */
    public List<String> getSemuaPredikat() {
        Set<String> hasil = new LinkedHashSet<>();
        for (Neuron n : neurons.values()) {
            for (Edge e : n.getOutgoing()) {
                hasil.add(e.getLabel());
            }
        }
        return new ArrayList<>(hasil);
    }

    /**
     * Menambahkan aturan komposisi baru dan langsung menyimpannya ke rules.txt.
     * predikat2 wajib diisi untuk tipe CHAIN, dan diabaikan untuk tipe TRANSITIVE.
     */
    public Aturan tambahAturan(String nama, Aturan.Tipe tipe, String predikat1, String predikat2, boolean excludeSelf) {
        if (nama == null || nama.isBlank()) {
            throw new KnowledgeGraphException("Nama aturan tidak boleh kosong.");
        }
        String key = nama.toLowerCase();
        if (aturanMap.containsKey(key)) {
            throw new KnowledgeGraphException("Aturan '" + nama + "' sudah ada.");
        }
        if (predikat1 == null || predikat1.isBlank()) {
            throw new KnowledgeGraphException("Predikat pertama tidak boleh kosong.");
        }
        if (tipe == Aturan.Tipe.CHAIN && (predikat2 == null || predikat2.isBlank())) {
            throw new KnowledgeGraphException("Predikat kedua wajib diisi untuk tipe CHAIN.");
        }

        Aturan aturan = new Aturan(nama, tipe, predikat1, tipe == Aturan.Tipe.CHAIN ? predikat2 : null, excludeSelf);
        aturanMap.put(key, aturan);
        simpanAturan();
        return aturan;
    }

    /** Menulis ulang seluruh rules.txt dari isi aturanMap saat ini. */
    private void simpanAturan() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rulesFilePath))) {
            for (Aturan a : aturanMap.values()) {
                if (a.getTipe() == Aturan.Tipe.CHAIN) {
                    writer.write("CHAIN|" + a.getNama() + "|" + a.getPredikat1() + "|" + a.getPredikat2() + "|" + a.isExcludeSelf());
                } else {
                    writer.write("TRANSITIVE|" + a.getNama() + "|" + a.getPredikat1());
                }
                writer.newLine();
            }
        } catch (IOException e) {
            throw new KnowledgeGraphException("Gagal menyimpan ke " + rulesFilePath + ": " + e.getMessage());
        }
    }

    /** Menghitung hasil satu aturan turunan (mis. "kakek/nenek dari") untuk satu neuron subjek. */
    public List<Neuron> cariRelasiTurunan(String subjekId, String namaAturan) {
        getNeuron(subjekId); // validasi subjek ada

        String key = namaAturan.toLowerCase();
        if (!aturanMap.containsKey(key)) {
            throw new KnowledgeGraphException("Aturan '" + namaAturan + "' tidak ditemukan di rules.txt.");
        }

        Set<String> idHasil = hitungRelasiTurunan(subjekId, key, 0);

        List<Neuron> hasil = new ArrayList<>();
        for (String id : idHasil) {
            Neuron n = neurons.get(id);
            if (n != null) hasil.add(n);
        }
        return hasil;
    }

    /** Semua target langsung dari subjek dengan predikat mentah (exact match, case-insensitive). */
    private List<String> targetDenganPredikat(String subjekId, String predikat) {
        Neuron n = neurons.get(subjekId);
        if (n == null) return Collections.emptyList();

        List<String> hasil = new ArrayList<>();
        for (Edge e : n.getOutgoing()) {
            if (e.getLabel().equalsIgnoreCase(predikat)) {
                hasil.add(e.getTargetId());
            }
        }
        return hasil;
    }

    /**
     * Menyelesaikan target dari sebuah "predikat" yang bisa berupa predikat mentah
     * ATAU nama aturan lain, sehingga aturan bisa disusun bertingkat.
     */
    private Set<String> resolveTargets(String subjekId, String predikatAtauAturan, int depth) {
        if (depth > MAX_KEDALAMAN_ATURAN) return Collections.emptySet();

        String key = predikatAtauAturan.toLowerCase();
        if (aturanMap.containsKey(key)) {
            return hitungRelasiTurunan(subjekId, key, depth + 1);
        }
        return new LinkedHashSet<>(targetDenganPredikat(subjekId, predikatAtauAturan));
    }

    /** Menghitung hasil satu aturan (CHAIN atau TRANSITIVE) untuk satu subjek. */
    private Set<String> hitungRelasiTurunan(String subjekId, String namaAturanLower, int depth) {
        Aturan aturan = aturanMap.get(namaAturanLower);
        if (aturan == null || depth > MAX_KEDALAMAN_ATURAN) {
            return Collections.emptySet();
        }

        return switch (aturan.getTipe()) {
            case CHAIN -> hitungChain(subjekId, aturan, depth);
            case TRANSITIVE -> hitungTransitive(subjekId, aturan, depth);
        };
    }

    private Set<String> hitungChain(String subjekId, Aturan aturan, int depth) {
        Set<String> tahap1 = resolveTargets(subjekId, aturan.getPredikat1(), depth);
        Set<String> hasil = new LinkedHashSet<>();

        for (String antara : tahap1) {
            hasil.addAll(resolveTargets(antara, aturan.getPredikat2(), depth));
        }
        if (aturan.isExcludeSelf()) {
            hasil.remove(subjekId);
        }
        return hasil;
    }

    private Set<String> hitungTransitive(String subjekId, Aturan aturan, int depth) {
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(resolveTargets(subjekId, aturan.getPredikat1(), depth));
        visited.addAll(queue);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String next : resolveTargets(current, aturan.getPredikat1(), depth)) {
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return visited;
    }
}