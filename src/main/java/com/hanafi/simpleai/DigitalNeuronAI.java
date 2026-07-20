package com.hanafi.simpleai;

import java.io.*;
import java.util.*;

/**
 * DigitalNeuronAI (Generic Rule Engine untuk Predicate Composition)
 * -------------------------------------------------------------------------------------
 * Relasi DASAR tetap triple (Subjek, Predikat, Objek), disimpan di neurons.txt.
 *
 * Relasi TURUNAN sekarang didefinisikan sebagai DATA di file rules.txt, bukan kode
 * Java. ada dua tipe aturan:
 *
 *   1) CHAIN  -> menyambung 2 predikat berurutan jadi relasi baru.
 *      Format: CHAIN|namaAturan|predikat1|predikat2|excludeSelf(true/false)
 *      Contoh: CHAIN|kakek/nenek dari|anak dari|anak dari|false
 *      Artinya: kakek/nenek dari X = ikuti "anak dari" dari X, lalu "anak dari" lagi.
 *
 *   2) TRANSITIVE -> menelusuri predikat yang SAMA berulang kali sampai habis
 *      (transitive closure). Cocok untuk kasus seperti komposisi bahan/BOM.
 *      Format: TRANSITIVE|namaAturan|predikat
 *      Contoh: TRANSITIVE|semua bahan dari|mengandung
 *      Artinya: kalau A mengandung B, B mengandung C, maka A "semua bahan dari" C juga.
 *
 * Predikat di dalam aturan BOLEH merujuk ke aturan lain (nama aturan lain), sehingga
 * aturan bisa disusun bertingkat. Contoh: "paman/bibi dari" = CHAIN(anak dari, saudara dari)
 * -- di mana "saudara dari" sendiri adalah aturan CHAIN lain.
 *
 * Karena semua ini DATA, domain apa pun (keluarga, resep, struktur organisasi, rantai
 * pasok) bisa dipakai tanpa mengubah kode -- cukup ganti isi rules.txt dan neurons.txt.
 */
public class DigitalNeuronAI {

    private static final String FILE_NAME = "neurons.txt";
    private static final String RULES_FILE = "rules.txt";
    private static final double DEFAULT_WEIGHT = 1.0;
    private static final double PENGUATAN = 0.1;
    private static final String EVENT_PREFIX = "event_";
    private static final int MAX_KEDALAMAN_ATURAN = 10; // pengaman dari aturan yang saling memanggil (cycle)

    private final Map<String, Neuron> neurons = new LinkedHashMap<>();
    private final Map<String, Aturan> aturanMap = new LinkedHashMap<>(); // key = nama aturan (lowercase)

    public static void main(String[] args) {
        DigitalNeuronAI ai = new DigitalNeuronAI();
        ai.loadFromFile();
        ai.loadRules();
        ai.runMenu();
    }

    static class Neuron {
        String id;
        String content;
        List<Edge> outgoing = new ArrayList<>();

        Neuron(String id, String content) {
            this.id = id;
            this.content = content;
        }
    }

    static class Edge {
        String targetId;
        String label;
        double weight;

        Edge(String targetId, String label, double weight) {
            this.targetId = targetId;
            this.label = label;
            this.weight = weight;
        }
    }

    /** Definisi satu aturan komposisi (relasi turunan) */
    static class Aturan {
        String nama;         // nama relasi turunan, misal "kakek/nenek dari"
        String tipe;         // "CHAIN" atau "TRANSITIVE"
        String predikat1;
        String predikat2;    // null untuk TRANSITIVE
        boolean excludeSelf; // hanya dipakai untuk CHAIN
    }

    // ================= MENU =================

    private void runMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== Digital Neuron AI ===");
            System.out.println("1. Tambah neuron (entitas baru)");
            System.out.println("2. Tambah relasi sederhana (Subjek -> Predikat -> Objek)");
            System.out.println("3. Tambah fakta majemuk (peristiwa dengan banyak detail)");
            System.out.println("4. Cari Objek   (input: Subjek + Predikat)");
            System.out.println("5. Cari Subjek  (input: Predikat + Objek)");
            System.out.println("6. Lihat informasi satu neuron");
            System.out.println("7. Aktivasi neuron (jelajah semua relasi terkait)");
            System.out.println("8. Lihat seluruh jaringan");
            System.out.println("9. Hapus neuron");
            System.out.println("10. Cari relasi turunan (berdasarkan rules.txt)");
            System.out.println("11. Lihat daftar aturan yang tersedia");
            System.out.println("12. Keluar");
            System.out.print("Pilih menu: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> tambahNeuron(scanner);
                case "2" -> tambahRelasi(scanner);
                case "3" -> tambahFaktaMajemuk(scanner);
                case "4" -> cariObjek(scanner);
                case "5" -> cariSubjek(scanner);
                case "6" -> lihatInformasiNeuron(scanner);
                case "7" -> aktivasiNeuron(scanner);
                case "8" -> tampilkanJaringan();
                case "9" -> hapusNeuron(scanner);
                case "10" -> cariRelasiTurunan(scanner);
                case "11" -> lihatDaftarAturan();
                case "12" -> {
                    running = false;
                    System.out.println("Jaringan disimpan. Sampai jumpa!");
                }
                default -> System.out.println("Pilihan tidak dikenali.");
            }
        }
        scanner.close();
    }

    // ================= INPUT DATA DASAR =================

    private void tambahNeuron(Scanner scanner) {
        System.out.print("Nama/ID neuron: ");
        String id = scanner.nextLine().trim();

        if (id.isEmpty()) {
            System.out.println("ID tidak boleh kosong.");
            return;
        }
        if (neurons.containsKey(id)) {
            System.out.println("Neuron dengan ID itu sudah ada.");
            return;
        }

        System.out.print("Isi pengetahuan/deskripsi: ");
        String content = scanner.nextLine().trim();
        neurons.put(id, new Neuron(id, content));

        saveToFile();
        System.out.println("Neuron '" + id + "' berhasil ditambahkan.");
    }

    private void tambahRelasi(Scanner scanner) {
        System.out.print("Subjek (ID neuron): ");
        String subjek = scanner.nextLine().trim();

        System.out.print("Predikat: ");
        String predikat = scanner.nextLine().trim();

        System.out.print("Objek (ID neuron): ");
        String objek = scanner.nextLine().trim();

        if (!neurons.containsKey(subjek) || !neurons.containsKey(objek)) {
            System.out.println("Subjek atau objek belum terdaftar sebagai neuron. Tambahkan dulu lewat menu 1.");
            return;
        }
        if (predikat.isEmpty()) {
            System.out.println("Predikat tidak boleh kosong.");
            return;
        }

        double bobot = mintaBobot(scanner);
        neurons.get(subjek).outgoing.add(new Edge(objek, predikat, bobot));
        saveToFile();
        System.out.printf("Fakta tersimpan: (%s, %s, %s)%n", subjek, predikat, objek);
    }

    private void tambahFaktaMajemuk(Scanner scanner) {
        System.out.print("Subjek (ID neuron): ");
        String subjek = scanner.nextLine().trim();

        if (!neurons.containsKey(subjek)) {
            System.out.println("Neuron subjek '" + subjek + "' belum terdaftar. Tambahkan dulu lewat menu 1.");
            return;
        }

        System.out.print("Predikat utama menuju peristiwa: ");
        String predikatUtama = scanner.nextLine().trim();
        if (predikatUtama.isEmpty()) {
            System.out.println("Predikat tidak boleh kosong.");
            return;
        }

        String idDefault = EVENT_PREFIX + hitungEventBerikutnya();
        System.out.print("ID untuk peristiwa ini (kosongkan untuk pakai default '" + idDefault + "'): ");
        String eventId = scanner.nextLine().trim();
        if (eventId.isEmpty()) eventId = idDefault;

        if (neurons.containsKey(eventId)) {
            System.out.println("ID peristiwa '" + eventId + "' sudah dipakai. Batal, coba lagi dengan ID lain.");
            return;
        }

        System.out.print("Deskripsi singkat peristiwa ini (boleh kosong): ");
        String deskripsi = scanner.nextLine().trim();

        neurons.put(eventId, new Neuron(eventId, deskripsi));
        neurons.get(subjek).outgoing.add(new Edge(eventId, predikatUtama, DEFAULT_WEIGHT));
        System.out.printf("Peristiwa '%s' dibuat: (%s, %s, %s)%n", eventId, subjek, predikatUtama, eventId);

        System.out.println("\nSekarang tambahkan detail untuk peristiwa ini.");
        System.out.println("(Kosongkan predikat kapan saja untuk selesai)");

        while (true) {
            System.out.print("\n  Predikat detail: ");
            String predikatDetail = scanner.nextLine().trim();
            if (predikatDetail.isEmpty()) break;

            System.out.print("  Objek/nilai detail: ");
            String objekDetail = scanner.nextLine().trim();
            if (objekDetail.isEmpty()) {
                System.out.println("  Objek tidak boleh kosong, detail ini dilewati.");
                continue;
            }

            if (!neurons.containsKey(objekDetail)) {
                System.out.print("  Neuron '" + objekDetail + "' belum ada. Buat sekarang dengan deskripsi (boleh kosong): ");
                String descObjek = scanner.nextLine().trim();
                neurons.put(objekDetail, new Neuron(objekDetail, descObjek));
            }

            neurons.get(eventId).outgoing.add(new Edge(objekDetail, predikatDetail, DEFAULT_WEIGHT));
            System.out.printf("  Detail tersimpan: (%s, %s, %s)%n", eventId, predikatDetail, objekDetail);
        }

        saveToFile();
        System.out.println("\nFakta majemuk selesai disimpan.");
    }

    private double mintaBobot(Scanner scanner) {
        System.out.print("Bobot awal (kosongkan untuk default " + DEFAULT_WEIGHT + "): ");
        String bobotInput = scanner.nextLine().trim();
        if (bobotInput.isEmpty()) return DEFAULT_WEIGHT;
        try {
            return Double.parseDouble(bobotInput);
        } catch (NumberFormatException e) {
            System.out.println("Bobot tidak valid, memakai default " + DEFAULT_WEIGHT);
            return DEFAULT_WEIGHT;
        }
    }

    private int hitungEventBerikutnya() {
        int max = 0;
        for (String id : neurons.keySet()) {
            if (id.startsWith(EVENT_PREFIX)) {
                try {
                    int n = Integer.parseInt(id.substring(EVENT_PREFIX.length()));
                    max = Math.max(max, n);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return max + 1;
    }

    // ================= QUERY INFORMASI (RELASI DASAR) =================

    private void cariObjek(Scanner scanner) {
        System.out.print("Subjek: ");
        String subjek = scanner.nextLine().trim();

        System.out.print("Predikat: ");
        String predikat = scanner.nextLine().trim();

        Neuron subjekNeuron = neurons.get(subjek);
        if (subjekNeuron == null) {
            System.out.println("Neuron subjek '" + subjek + "' tidak ditemukan.");
            return;
        }

        boolean ketemu = false;
        System.out.println("\n>> Hasil pencarian: \"" + subjek + " " + predikat + " ...?\"");
        for (Edge e : subjekNeuron.outgoing) {
            if (e.label.equalsIgnoreCase(predikat)) {
                Neuron objekNeuron = neurons.get(e.targetId);
                if (objekNeuron != null) {
                    System.out.println("- " + objekNeuron.id + ": " + objekNeuron.content);
                    tampilkanDetailJikaAda(objekNeuron, "    ");
                    ketemu = true;
                }
            }
        }
        if (!ketemu) {
            System.out.println("Tidak ditemukan fakta \"" + subjek + " " + predikat + "\" di jaringan.");
        }
    }

    private void cariSubjek(Scanner scanner) {
        System.out.print("Predikat: ");
        String predikat = scanner.nextLine().trim();

        System.out.print("Objek: ");
        String objek = scanner.nextLine().trim();

        boolean ketemu = false;
        System.out.println("\n>> Hasil pencarian: \"...? " + predikat + " " + objek + "\"");
        for (Neuron n : neurons.values()) {
            for (Edge e : n.outgoing) {
                if (e.label.equalsIgnoreCase(predikat) && e.targetId.equalsIgnoreCase(objek)) {
                    System.out.println("- " + n.id + ": " + n.content);
                    ketemu = true;
                }
            }
        }
        if (!ketemu) {
            System.out.println("Tidak ditemukan fakta \"" + predikat + " " + objek + "\" di jaringan.");
        }
    }

    private void tampilkanDetailJikaAda(Neuron n, String indent) {
        if (n.outgoing.isEmpty()) return;
        System.out.println(indent + "Detail tambahan:");
        for (Edge e : n.outgoing) {
            Neuron target = neurons.get(e.targetId);
            if (target != null) {
                System.out.println(indent + "  " + e.label + ": " + target.id + " (" + target.content + ")");
            }
        }
    }

    private void lihatInformasiNeuron(Scanner scanner) {
        System.out.print("Masukkan ID neuron: ");
        String id = scanner.nextLine().trim();

        Neuron n = neurons.get(id);
        if (n == null) {
            System.out.println("Neuron '" + id + "' tidak ditemukan.");
            return;
        }
        System.out.println("\n>> " + n.id);
        System.out.println("   " + n.content);
        tampilkanDetailJikaAda(n, "   ");
    }

    private void aktivasiNeuron(Scanner scanner) {
        System.out.print("Masukkan ID neuron yang ingin diaktifkan: ");
        String startId = scanner.nextLine().trim();

        Neuron start = neurons.get(startId);
        if (start == null) {
            System.out.println("Neuron tidak ditemukan.");
            return;
        }

        System.out.println("\n>> Neuron utama: " + start.id);
        System.out.println("   Pengetahuan: " + start.content);

        if (start.outgoing.isEmpty()) {
            System.out.println("(Belum ada relasi keluar dari neuron ini)");
            return;
        }

        int maxDepth = 3;
        Set<String> visited = new HashSet<>();
        visited.add(start.id);

        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> depthMap = new HashMap<>();
        queue.add(start.id);
        depthMap.put(start.id, 0);

        System.out.println("\n>> Informasi terkait (diurutkan bobot tertinggi):");
        List<Edge> dilewati = new ArrayList<>();

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            int depth = depthMap.get(currentId);
            if (depth >= maxDepth) continue;

            Neuron current = neurons.get(currentId);
            List<Edge> edges = new ArrayList<>(current.outgoing);
            edges.sort((e1, e2) -> Double.compare(e2.weight, e1.weight));

            String indent = "  ".repeat(depth + 1);
            for (Edge edge : edges) {
                Neuron target = neurons.get(edge.targetId);
                if (target == null) continue;

                System.out.printf("%s%s --[%s]--> %s: %s%n",
                        indent, current.id, edge.label, target.id, target.content);

                dilewati.add(edge);

                if (!visited.contains(edge.targetId)) {
                    visited.add(edge.targetId);
                    depthMap.put(edge.targetId, depth + 1);
                    queue.add(edge.targetId);
                }
            }
        }

        for (Edge e : dilewati) {
            e.weight += PENGUATAN;
        }
        if (!dilewati.isEmpty()) {
            saveToFile();
        }
    }

    private void tampilkanJaringan() {
        if (neurons.isEmpty()) {
            System.out.println("Jaringan masih kosong.");
            return;
        }
        System.out.println("--- Seluruh Neuron ---");
        for (Neuron n : neurons.values()) {
            System.out.println("* " + n.id + ": " + n.content);
        }

        System.out.println("\n--- Seluruh Fakta (Subjek -> Predikat -> Objek) ---");
        boolean adaFakta = false;
        for (Neuron n : neurons.values()) {
            for (Edge e : n.outgoing) {
                System.out.printf("(%s, %s, %s) [bobot %.2f]%n", n.id, e.label, e.targetId, e.weight);
                adaFakta = true;
            }
        }
        if (!adaFakta) {
            System.out.println("(Belum ada fakta/relasi)");
        }
    }

    private void hapusNeuron(Scanner scanner) {
        System.out.print("ID neuron yang ingin dihapus: ");
        String id = scanner.nextLine().trim();

        if (neurons.remove(id) == null) {
            System.out.println("Neuron tidak ditemukan.");
            return;
        }

        for (Neuron n : neurons.values()) {
            n.outgoing.removeIf(edge -> edge.targetId.equals(id));
        }

        saveToFile();
        System.out.println("Neuron '" + id + "' dan seluruh relasi terkait berhasil dihapus.");
    }

    // ================= GENERIC RULE ENGINE (RELASI TURUNAN) =================

    /** Semua target langsung dari subjek dengan predikat mentah (exact match, case-insensitive) */
    private List<String> targetDenganPredikat(String subjekId, String predikat) {
        List<String> hasil = new ArrayList<>();
        Neuron n = neurons.get(subjekId);
        if (n == null) return hasil;
        for (Edge e : n.outgoing) {
            if (e.label.equalsIgnoreCase(predikat)) {
                hasil.add(e.targetId);
            }
        }
        return hasil;
    }

    /**
     * Menyelesaikan target dari sebuah "predikat" yang bisa berupa predikat mentah
     * (langsung dari data) ATAU nama aturan lain (relasi turunan), sehingga aturan
     * bisa disusun bertingkat (satu aturan memakai aturan lain sebagai komponennya).
     */
    private Set<String> resolveTargets(String subjekId, String predikatAtauAturan, int depth) {
        if (depth > MAX_KEDALAMAN_ATURAN) return Collections.emptySet();

        String key = predikatAtauAturan.toLowerCase();
        if (aturanMap.containsKey(key)) {
            return hitungRelasiTurunan(subjekId, key, depth + 1);
        }
        return new LinkedHashSet<>(targetDenganPredikat(subjekId, predikatAtauAturan));
    }

    /** Menghitung hasil satu aturan (CHAIN atau TRANSITIVE) untuk satu subjek */
    private Set<String> hitungRelasiTurunan(String subjekId, String namaAturanLower, int depth) {
        Aturan a = aturanMap.get(namaAturanLower);
        if (a == null || depth > MAX_KEDALAMAN_ATURAN) return Collections.emptySet();

        if (a.tipe.equals("CHAIN")) {
            Set<String> tahap1 = resolveTargets(subjekId, a.predikat1, depth);
            Set<String> hasil = new LinkedHashSet<>();
            for (String antara : tahap1) {
                hasil.addAll(resolveTargets(antara, a.predikat2, depth));
            }
            if (a.excludeSelf) hasil.remove(subjekId);
            return hasil;
        }

        if (a.tipe.equals("TRANSITIVE")) {
            Set<String> visited = new LinkedHashSet<>();
            Deque<String> queue = new ArrayDeque<>(resolveTargets(subjekId, a.predikat1, depth));
            visited.addAll(queue);

            while (!queue.isEmpty()) {
                String current = queue.poll();
                for (String next : resolveTargets(current, a.predikat1, depth)) {
                    if (!visited.contains(next)) {
                        visited.add(next);
                        queue.add(next);
                    }
                }
            }
            return visited;
        }

        return Collections.emptySet();
    }

    private void cariRelasiTurunan(Scanner scanner) {
        if (aturanMap.isEmpty()) {
            System.out.println("Belum ada aturan di rules.txt. Tambahkan aturan dulu (lihat contoh format di komentar kode).");
            return;
        }

        lihatDaftarAturan();
        System.out.print("\nMasukkan nama aturan yang ingin dipakai: ");
        String namaAturan = scanner.nextLine().trim().toLowerCase();

        if (!aturanMap.containsKey(namaAturan)) {
            System.out.println("Aturan '" + namaAturan + "' tidak ditemukan.");
            return;
        }

        System.out.print("ID neuron subjek: ");
        String id = scanner.nextLine().trim();

        if (!neurons.containsKey(id)) {
            System.out.println("Neuron '" + id + "' tidak ditemukan.");
            return;
        }

        Set<String> hasil = hitungRelasiTurunan(id, namaAturan, 0);

        System.out.println("\n>> " + aturanMap.get(namaAturan).nama + " " + id + ":");
        if (hasil.isEmpty()) {
            System.out.println("(Tidak ditemukan)");
            return;
        }
        for (String hid : hasil) {
            Neuron n = neurons.get(hid);
            if (n != null) {
                System.out.println("- " + n.id + ": " + n.content);
            }
        }
    }

    private void lihatDaftarAturan() {
        if (aturanMap.isEmpty()) {
            System.out.println("Belum ada aturan terdaftar di rules.txt.");
            return;
        }
        System.out.println("--- Daftar Aturan (rules.txt) ---");
        for (Aturan a : aturanMap.values()) {
            if (a.tipe.equals("CHAIN")) {
                System.out.printf("- %s  [CHAIN: %s -> %s%s]%n",
                        a.nama, a.predikat1, a.predikat2, a.excludeSelf ? ", exclude self" : "");
            } else {
                System.out.printf("- %s  [TRANSITIVE: %s]%n", a.nama, a.predikat1);
            }
        }
    }

    // ================= PENYIMPANAN FILE =================

    private void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Neuron n : neurons.values()) {
                writer.write("NEURON|" + n.id + "|" + n.content);
                writer.newLine();
            }
            for (Neuron n : neurons.values()) {
                for (Edge e : n.outgoing) {
                    writer.write("LINK|" + n.id + "|" + e.label + "|" + e.targetId + "|" + e.weight);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Gagal menyimpan jaringan: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            List<String[]> pendingLinks = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", 5);

                if (parts[0].equals("NEURON") && parts.length >= 3) {
                    neurons.put(parts[1], new Neuron(parts[1], parts[2]));
                } else if (parts[0].equals("LINK") && parts.length == 5) {
                    pendingLinks.add(parts);
                }
            }

            for (String[] parts : pendingLinks) {
                String subjek = parts[1];
                String predikat = parts[2];
                String objek = parts[3];
                double bobot;
                try {
                    bobot = Double.parseDouble(parts[4]);
                } catch (NumberFormatException e) {
                    bobot = DEFAULT_WEIGHT;
                }
                if (neurons.containsKey(subjek) && neurons.containsKey(objek)) {
                    neurons.get(subjek).outgoing.add(new Edge(objek, predikat, bobot));
                }
            }
        } catch (IOException e) {
            System.out.println("Gagal membaca jaringan: " + e.getMessage());
        }
    }

    /**
     * Memuat aturan komposisi dari rules.txt. Format per baris:
     *   CHAIN|namaAturan|predikat1|predikat2|excludeSelf(true/false)
     *   TRANSITIVE|namaAturan|predikat
     * Baris kosong atau diawali '#' diabaikan (komentar).
     */
    private void loadRules() {
        File file = new File(RULES_FILE);
        if (!file.exists()) {
            System.out.println("(Info: " + RULES_FILE + " tidak ditemukan, relasi turunan belum aktif)");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|");
                Aturan a = new Aturan();

                if (parts[0].equalsIgnoreCase("CHAIN") && parts.length >= 4) {
                    a.tipe = "CHAIN";
                    a.nama = parts[1];
                    a.predikat1 = parts[2];
                    a.predikat2 = parts[3];
                    a.excludeSelf = parts.length >= 5 && parts[4].equalsIgnoreCase("true");
                    aturanMap.put(a.nama.toLowerCase(), a);
                } else if (parts[0].equalsIgnoreCase("TRANSITIVE") && parts.length >= 3) {
                    a.tipe = "TRANSITIVE";
                    a.nama = parts[1];
                    a.predikat1 = parts[2];
                    aturanMap.put(a.nama.toLowerCase(), a);
                }
            }
        } catch (IOException e) {
            System.out.println("Gagal membaca rules.txt: " + e.getMessage());
        }
    }
}