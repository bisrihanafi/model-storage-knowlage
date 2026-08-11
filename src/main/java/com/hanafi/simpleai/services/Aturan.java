/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hanafi.simpleai.services;

/**
 * Definisi satu aturan komposisi (relasi turunan), dibaca dari rules.txt.
 *
 * Dua tipe aturan yang didukung: CHAIN - menyambung 2 predikat berurutan (boleh
 * beda) jadi relasi baru. Contoh: kakek/nenek dari = (anak dari) lalu (anak
 * dari). TRANSITIVE - menelusuri 1 predikat yang sama berulang kali sampai
 * habis. Contoh: semua bahan dari = transitive closure dari "mengandung".
 *
 * predikat1/predikat2 boleh berupa predikat mentah dari data, ATAU nama aturan
 * lain, sehingga aturan bisa disusun bertingkat.
 */
public class Aturan {

    public enum Tipe {
        CHAIN,
        TRANSITIVE
    }

    private final String nama;
    private final Tipe tipe;
    private final String predikat1;
    private final String predikat2; // null untuk TRANSITIVE
    private final boolean excludeSelf; // hanya relevan untuk CHAIN

    public Aturan(String nama, Tipe tipe, String predikat1, String predikat2, boolean excludeSelf) {
        this.nama = nama;
        this.tipe = tipe;
        this.predikat1 = predikat1;
        this.predikat2 = predikat2;
        this.excludeSelf = excludeSelf;
    }

    public String getNama() {
        return nama;
    }

    public Tipe getTipe() {
        return tipe;
    }

    public String getPredikat1() {
        return predikat1;
    }

    public String getPredikat2() {
        return predikat2;
    }

    public boolean isExcludeSelf() {
        return excludeSelf;
    }

    @Override
    public String toString() {
        if (tipe == Tipe.CHAIN) {
            return nama + " [CHAIN: " + predikat1 + " -> " + predikat2
                    + (excludeSelf ? ", exclude self" : "") + "]";
        }
        return nama + " [TRANSITIVE: " + predikat1 + "]";
    }
}
