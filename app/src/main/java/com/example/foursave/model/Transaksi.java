package com.example.foursave.model;

public class Transaksi {
    private String id;
    private String deskripsi;
    // Menggunakan Object agar bisa menerima String ("50000") atau Double (50000)
    private Object jumlah;
    private String tipe;
    private String tanggal;
    private String kategori;

    public Transaksi() {
        // Constructor kosong diperlukan untuk Firebase
    }

    public Transaksi(String id, String deskripsi, double jumlah, String tipe, String tanggal, String kategori) {
        this.id = id;
        this.deskripsi = deskripsi;
        this.jumlah = jumlah;
        this.tipe = tipe;
        this.tanggal = tanggal;
        this.kategori = kategori;
    }

    public String getId() {
        return id;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    // Getter pintar: Otomatis ubah ke double, apapun format aslinya di database
    public double getJumlah() {
        if (jumlah == null) return 0;

        if (jumlah instanceof Double) {
            return (Double) jumlah;
        } else if (jumlah instanceof Long) {
            return ((Long) jumlah).doubleValue();
        } else if (jumlah instanceof String) {
            try {
                return Double.parseDouble((String) jumlah);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public void setJumlah(Object jumlah) {
        this.jumlah = jumlah;
    }

    public String getTipe() {
        return tipe;
    }

    public String getTanggal() {
        return tanggal;
    }

    public String getKategori() {
        return kategori;
    }
}