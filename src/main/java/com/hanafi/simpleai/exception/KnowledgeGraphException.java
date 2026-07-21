/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hanafi.simpleai.exception;

/**
 * Exception khusus untuk semua kondisi error dari KnowledgeGraphService (neuron
 * tidak ditemukan, ID duplikat, aturan tidak dikenal, dsb).
 *
 * Dibuat unchecked (extends RuntimeException) supaya pemanggil - baik
 * ConsoleApp maupun nanti REST API controller - bebas menangani sesuai
 * kebutuhan masing-masing (misal: ConsoleApp cetak ke layar, API controller
 * ubah jadi response HTTP 400/404).
 */
public class KnowledgeGraphException extends RuntimeException {

    public KnowledgeGraphException(String message) {
        super(message);
    }
}
