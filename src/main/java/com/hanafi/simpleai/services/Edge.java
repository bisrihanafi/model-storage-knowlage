/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hanafi.simpleai.services;

/**
 * Satu relasi berarah dari satu neuron (subjek) ke neuron lain (objek), dengan
 * label (predikat) dan bobot (kekuatan relasi).
 *
 * Contoh: (Saya) --[memiliki, bobot 1.0]--> (Mobil)
 */
public class Edge {

    private final String targetId;
    private final String label;
    private double weight;

    public Edge(String targetId, String label, double weight) {
        this.targetId = targetId;
        this.label = label;
        this.weight = weight;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getLabel() {
        return label;
    }

    public double getWeight() {
        return weight;
    }

    /**
     * Memperkuat bobot relasi ini, dipakai saat neuron diaktifkan (efek
     * Hebbian).
     */
    void perkuatBobot(double tambahan) {
        this.weight += tambahan;
    }

    @Override
    public String toString() {
        return "--[" + label + ", bobot " + weight + "]--> " + targetId;
    }
}
