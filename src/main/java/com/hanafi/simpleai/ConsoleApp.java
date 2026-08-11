/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hanafi.simpleai;

import com.hanafi.simpleai.exception.KnowledgeGraphException;
import com.hanafi.simpleai.services.Aturan;
import com.hanafi.simpleai.services.Edge;
import com.hanafi.simpleai.services.KnowledgeGraphService;
import com.hanafi.simpleai.services.Neuron;
import java.util.List;
import java.util.Scanner;

/**
 * Aplikasi menu di terminal untuk Digital Neuron AI.
 *
 * Kelas ini HANYA menangani input dari user dan menampilkan output ke layar.
 * Semua logika (validasi, penyimpanan, query, rule engine) ada di
 * KnowledgeGraphService - kelas ini cuma "pengemudi" yang memanggilnya.
 */
public class ConsoleApp {

    private final KnowledgeGraphService service;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleApp(KnowledgeGraphService service) {
        this.service = service;
    }

    public static void main(String[] args) {
        KnowledgeGraphService service = new KnowledgeGraphService();
        service.muatData();
        service.muatAturan();

        new ConsoleApp(service).run();
    }

    public void run() {
        boolean berjalan = true;
        while (berjalan) {
            tampilkanMenu();
            String pilihan = scanner.nextLine().trim();

            try {
                switch (pilihan) {
                    case "1" ->
                        menuTambahNeuron();
                    case "2" ->
                        menuTambahRelasi();
                    case "3" ->
                        menuTambahFaktaMajemuk();
                    case "4" ->
                        menuCariObjek();
                    case "5" ->
                        menuCariSubjek();
                    case "6" ->
                        menuLihatInformasiNeuron();
                    case "7" ->
                        menuAktivasiNeuron();
                    case "8" ->
                        menuTampilkanJaringan();
                    case "9" ->
                        menuHapusNeuron();
                    case "10" ->
                        menuCariRelasiTurunan();
                    case "11" ->
                        menuLihatDaftarAturan();
                    case "12" ->
                        menuTambahAturan();
                    case "13" ->
                        menuCariRelasiAntara();
                    case "14" ->
                        menuCariAturanPenghubung();
                    case "15" -> {
                        berjalan = false;
                        System.out.println("Jaringan disimpan. Sampai jumpa!");
                    }
                    default ->
                        System.out.println("Pilihan tidak dikenali.");
                }
            } catch (KnowledgeGraphException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
        scanner.close();
    }

    private void tampilkanMenu() {
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
        System.out.println("12. Tambah aturan baru");
        System.out.println("13. Cari relasi antara dua neuron (dua arah)");
        System.out.println("14. Cari aturan yang menghubungkan dua neuron");
        System.out.println("15. Keluar");
        System.out.print("Pilih menu: ");
    }

    // ================= MENU HANDLERS =================
    private void menuTambahNeuron() {
        System.out.print("Nama/ID neuron: ");
        String id = scanner.nextLine().trim();

        System.out.print("Isi pengetahuan/deskripsi: ");
        String content = scanner.nextLine().trim();

        Neuron neuron = service.tambahNeuron(id, content);
        System.out.println("Neuron '" + neuron.getId() + "' berhasil ditambahkan.");
    }

    private void menuTambahRelasi() {
        System.out.print("Subjek (ID neuron): ");
        String subjek = scanner.nextLine().trim();

        System.out.print("Predikat: ");
        String predikat = scanner.nextLine().trim();

        System.out.print("Objek (ID neuron): ");
        String objek = scanner.nextLine().trim();

        Double bobot = mintaBobotOpsional();

        service.tambahRelasi(subjek, predikat, objek, bobot);
        System.out.printf("Fakta tersimpan: (%s, %s, %s)%n", subjek, predikat, objek);
    }

    private void menuTambahFaktaMajemuk() {
        System.out.print("Subjek (ID neuron): ");
        String subjek = scanner.nextLine().trim();

        System.out.print("Predikat utama menuju peristiwa: ");
        String predikatUtama = scanner.nextLine().trim();

        System.out.print("ID untuk peristiwa ini (kosongkan untuk auto-generate): ");
        String eventIdInput = scanner.nextLine().trim();

        System.out.print("Deskripsi singkat peristiwa ini (boleh kosong): ");
        String deskripsi = scanner.nextLine().trim();

        String eventId = service.buatPeristiwa(subjek, predikatUtama, eventIdInput, deskripsi);
        System.out.printf("Peristiwa '%s' dibuat: (%s, %s, %s)%n", eventId, subjek, predikatUtama, eventId);

        System.out.println("\nSekarang tambahkan detail untuk peristiwa ini.");
        System.out.println("(Kosongkan predikat kapan saja untuk selesai)");

        while (true) {
            System.out.print("\n  Predikat detail: ");
            String predikatDetail = scanner.nextLine().trim();
            if (predikatDetail.isEmpty()) {
                break;
            }

            System.out.print("  Objek/nilai detail: ");
            String objekDetail = scanner.nextLine().trim();

            String deskripsiObjekBaru = null;
            if (!service.adaNeuron(objekDetail)) {
                System.out.print("  Neuron '" + objekDetail + "' belum ada. Deskripsi singkat (boleh kosong): ");
                deskripsiObjekBaru = scanner.nextLine().trim();
            }

            service.tambahDetailPeristiwa(eventId, predikatDetail, objekDetail, deskripsiObjekBaru);
            System.out.printf("  Detail tersimpan: (%s, %s, %s)%n", eventId, predikatDetail, objekDetail);
        }

        System.out.println("\nFakta majemuk selesai disimpan.");
    }

    private void menuCariObjek() {
        System.out.print("Subjek: ");
        String subjek = scanner.nextLine().trim();

        System.out.print("Predikat: ");
        String predikat = scanner.nextLine().trim();

        List<Neuron> hasil = service.cariObjek(subjek, predikat);

        System.out.println("\n>> Hasil pencarian: \"" + subjek + " " + predikat + " ...?\"");
        if (hasil.isEmpty()) {
            System.out.println("Tidak ditemukan fakta \"" + subjek + " " + predikat + "\" di jaringan.");
            return;
        }
        for (Neuron n : hasil) {
            System.out.println("- " + n.getId() + ": " + n.getContent());
            tampilkanDetailJikaAda(n, "    ");
        }
    }

    private void menuCariSubjek() {
        System.out.print("Predikat: ");
        String predikat = scanner.nextLine().trim();

        System.out.print("Objek: ");
        String objek = scanner.nextLine().trim();

        List<Neuron> hasil = service.cariSubjek(predikat, objek);

        System.out.println("\n>> Hasil pencarian: \"...? " + predikat + " " + objek + "\"");
        if (hasil.isEmpty()) {
            System.out.println("Tidak ditemukan fakta \"" + predikat + " " + objek + "\" di jaringan.");
            return;
        }
        for (Neuron n : hasil) {
            System.out.println("- " + n.getId() + ": " + n.getContent());
        }
    }

    private void menuLihatInformasiNeuron() {
        System.out.print("Masukkan ID neuron: ");
        String id = scanner.nextLine().trim();

        Neuron n = service.getNeuron(id);
        System.out.println("\n>> " + n.getId());
        System.out.println("   " + n.getContent());
        tampilkanDetailJikaAda(n, "   ");
    }

    private void menuAktivasiNeuron() {
        System.out.print("Masukkan ID neuron yang ingin diaktifkan: ");
        String id = scanner.nextLine().trim();

        Neuron start = service.getNeuron(id);
        System.out.println("\n>> Neuron utama: " + start.getId());
        System.out.println("   Pengetahuan: " + start.getContent());

        List<KnowledgeGraphService.LangkahAktivasi> langkahList = service.aktivasiNeuron(id);

        if (langkahList.isEmpty()) {
            System.out.println("(Belum ada relasi keluar dari neuron ini)");
            return;
        }

        System.out.println("\n>> Informasi terkait:");
        for (KnowledgeGraphService.LangkahAktivasi langkah : langkahList) {
            Neuron tujuan = service.getNeuron(langkah.tujuanId());
            String indent = "  ".repeat(langkah.kedalaman());
            System.out.printf("%s%s --[%s]--> %s: %s%n",
                    indent, langkah.asalId(), langkah.predikat(), tujuan.getId(), tujuan.getContent());
        }
    }

    private void menuTampilkanJaringan() {
        List<Neuron> semuaNeuron = service.getSemuaNeuron();
        if (semuaNeuron.isEmpty()) {
            System.out.println("Jaringan masih kosong.");
            return;
        }

        System.out.println("--- Seluruh Neuron ---");
        for (Neuron n : semuaNeuron) {
            System.out.println("* " + n.getId() + ": " + n.getContent());
        }

        System.out.println("\n--- Seluruh Fakta (Subjek -> Predikat -> Objek) ---");
        List<KnowledgeGraphService.Fakta> semuaFakta = service.getSemuaFakta();
        if (semuaFakta.isEmpty()) {
            System.out.println("(Belum ada fakta/relasi)");
            return;
        }
        for (KnowledgeGraphService.Fakta f : semuaFakta) {
            System.out.printf("(%s, %s, %s) [bobot %.2f]%n", f.subjekId(), f.predikat(), f.objekId(), f.bobot());
        }
    }

    private void menuHapusNeuron() {
        System.out.print("ID neuron yang ingin dihapus: ");
        String id = scanner.nextLine().trim();

        service.hapusNeuron(id);
        System.out.println("Neuron '" + id + "' dan seluruh relasi terkait berhasil dihapus.");
    }

    private void menuCariRelasiTurunan() {
        List<Aturan> daftarAturan = service.getSemuaAturan();
        if (daftarAturan.isEmpty()) {
            System.out.println("Belum ada aturan di rules.txt.");
            return;
        }

        cetakDaftarAturan(daftarAturan);
        System.out.print("\nMasukkan nama aturan yang ingin dipakai: ");
        String namaAturan = scanner.nextLine().trim();

        System.out.print("ID neuron subjek: ");
        String id = scanner.nextLine().trim();

        List<Neuron> hasil = service.cariRelasiTurunan(id, namaAturan);

        System.out.println("\n>> " + namaAturan + " " + id + ":");
        if (hasil.isEmpty()) {
            System.out.println("(Tidak ditemukan)");
            return;
        }
        for (Neuron n : hasil) {
            System.out.println("- " + n.getId() + ": " + n.getContent());
        }
    }

    private void menuLihatDaftarAturan() {
        cetakDaftarAturan(service.getSemuaAturan());
    }

    private void menuTambahAturan() {
        System.out.println("\n--- Predikat yang sudah pernah dipakai di data ---");
        List<String> predikatTersedia = service.getSemuaPredikat();
        if (predikatTersedia.isEmpty()) {
            System.out.println("(Belum ada predikat apa pun di data)");
        } else {
            for (String p : predikatTersedia) {
                System.out.println("- " + p);
            }
        }

        System.out.println("\n--- Aturan yang sudah ada ---");
        cetakDaftarAturan(service.getSemuaAturan());

        System.out.print("\nNama aturan baru: ");
        String nama = scanner.nextLine().trim();

        System.out.print("Tipe (CHAIN/TRANSITIVE): ");
        String tipeInput = scanner.nextLine().trim().toUpperCase();

        Aturan.Tipe tipe;
        try {
            tipe = Aturan.Tipe.valueOf(tipeInput);
        } catch (IllegalArgumentException e) {
            System.out.println("Tipe tidak dikenali. Gunakan CHAIN atau TRANSITIVE.");
            return;
        }

        System.out.print("Predikat pertama: ");
        String predikat1 = scanner.nextLine().trim();

        String predikat2 = null;
        boolean excludeSelf = false;

        if (tipe == Aturan.Tipe.CHAIN) {
            System.out.print("Predikat kedua: ");
            predikat2 = scanner.nextLine().trim();

            System.out.print("Kecualikan diri sendiri dari hasil? (y/n): ");
            excludeSelf = scanner.nextLine().trim().equalsIgnoreCase("y");
        }

        Aturan aturanBaru = service.tambahAturan(nama, tipe, predikat1, predikat2, excludeSelf);
        System.out.println("\nAturan berhasil ditambahkan: " + aturanBaru);
    }

    private void menuCariRelasiAntara() {
        System.out.print("ID neuron pertama: ");
        String idA = scanner.nextLine().trim();

        System.out.print("ID neuron kedua: ");
        String idB = scanner.nextLine().trim();

        List<KnowledgeGraphService.Fakta> hasil = service.cariRelasiAntara(idA, idB);

        System.out.println("\n>> Relasi antara " + idA + " dan " + idB + ":");
        if (hasil.isEmpty()) {
            System.out.println("(Tidak ada relasi langsung di antara keduanya)");
            return;
        }
        for (KnowledgeGraphService.Fakta f : hasil) {
            System.out.printf("%s --[%s, bobot %.2f]--> %s%n", f.subjekId(), f.predikat(), f.bobot(), f.objekId());
        }
    }

    private void menuCariAturanPenghubung() {
        System.out.print("ID neuron pertama: ");
        String idA = scanner.nextLine().trim();

        System.out.print("ID neuron kedua: ");
        String idB = scanner.nextLine().trim();

        List<KnowledgeGraphService.AturanCocok> hasil = service.cariAturanYangMenghubungkan(idA, idB);

        System.out.println("\n>> Aturan yang menghubungkan " + idA + " dan " + idB + ":");
        if (hasil.isEmpty()) {
            System.out.println("(Tidak ada aturan di rules.txt yang cocok menghubungkan keduanya)");
            return;
        }
        for (KnowledgeGraphService.AturanCocok ac : hasil) {
            System.out.printf("%s adalah \"%s\" dari %s%n", ac.tujuanId(), ac.namaAturan(), ac.asalId());
        }
    }

    // ================= HELPER TAMPILAN =================
    private void cetakDaftarAturan(List<Aturan> daftar) {
        if (daftar.isEmpty()) {
            System.out.println("Belum ada aturan terdaftar di rules.txt.");
            return;
        }
        System.out.println("--- Daftar Aturan (rules.txt) ---");
        for (Aturan a : daftar) {
            System.out.println("- " + a);
        }
    }

    private void tampilkanDetailJikaAda(Neuron n, String indent) {
        if (n.getOutgoing().isEmpty()) {
            return;
        }
        System.out.println(indent + "Detail tambahan:");
        for (Edge e : n.getOutgoing()) {
            if (service.adaNeuron(e.getTargetId())) {
                Neuron target = service.getNeuron(e.getTargetId());
                System.out.println(indent + "  " + e.getLabel() + ": " + target.getId() + " (" + target.getContent() + ")");
            }
        }
    }

    private Double mintaBobotOpsional() {
        System.out.print("Bobot awal (kosongkan untuk default): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            System.out.println("Bobot tidak valid, memakai default.");
            return null;
        }
    }
}
