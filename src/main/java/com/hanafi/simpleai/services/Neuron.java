/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hanafi.simpleai.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Satu neuron / satu entitas pengetahuan di dalam jaringan. Contoh: "Saya",
 * "Mobil", "Hasan", "Kue Coklat".
 *
 * Sebuah neuron menyimpan isi/deskripsinya sendiri, dan daftar relasi KELUAR
 * (edge) di mana neuron ini berperan sebagai SUBJEK.
 */
public class Neuron {

    private final String id;
    private String content;
    private final List<Edge> outgoing = new ArrayList<>();

    public Neuron(String id, String content) {
        this.id = id;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Daftar relasi keluar (read-only) - gunakan tambahRelasiKeluar() untuk
     * menambah.
     */
    public List<Edge> getOutgoing() {
        return Collections.unmodifiableList(outgoing);
    }

    /**
     * Dipakai secara internal oleh KnowledgeGraphService saat membangun/memuat
     * relasi.
     */
    void tambahRelasiKeluar(Edge edge) {
        outgoing.add(edge);
    }

    /**
     * Dipakai secara internal saat menghapus neuron lain yang jadi target
     * relasi ini.
     */
    void hapusRelasiKeKe(String targetId) {
        outgoing.removeIf(edge -> edge.getTargetId().equals(targetId));
    }

    @Override
    public String toString() {
        return id + ": " + content;
    }
}
