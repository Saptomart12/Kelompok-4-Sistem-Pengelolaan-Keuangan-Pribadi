package com.example.foursave.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.foursave.R;
import com.example.foursave.model.Transaksi;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    // --- VARIABEL UI ---
    private TextView tvName; // Untuk Sapaan Nama User
    private TextView tvTotalSaldo, tvTotalPemasukan, tvTotalPengeluaran;
    private LinearLayout layoutListTransaksi;
    private ExtendedFloatingActionButton btnAddTransaction;

    // --- VARIABEL FIREBASE ---
    private DatabaseReference databaseReference;
    // Pastikan URL Database ini sesuai dengan yang ada di google-services.json Anda
    private final String DB_URL = "https://foursave-keren-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private FirebaseAuth mAuth;
    private String userID;

    // --- VARIABEL DATA ---
    private List<Transaksi> transaksiList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // 1. Inisialisasi Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userID = currentUser.getUid();
            // Referensi Database: users -> {userID} -> transaksi
            databaseReference = FirebaseDatabase.getInstance(DB_URL).getReference("transaksi").child(userID);
        }

        transaksiList = new ArrayList<>();

        // 2. Binding View (Menghubungkan variabel dengan ID di XML)
        tvName = view.findViewById(R.id.tvName); // Pastikan ID ini ada di XML
        tvTotalSaldo = view.findViewById(R.id.tvTotalSaldo);
        tvTotalPemasukan = view.findViewById(R.id.tvTotalPemasukan);
        tvTotalPengeluaran = view.findViewById(R.id.tvTotalPengeluaran);
        layoutListTransaksi = view.findViewById(R.id.layoutListTransaksi);
        btnAddTransaction = view.findViewById(R.id.btnAddTransaction);

        // 3. Load Data User (Untuk Sapaan Nama)
        loadUserName();

        // 4. Load Data Transaksi dari Firebase
        loadDataFirebase();

        // 5. Aksi Tombol Tambah (FAB)
        btnAddTransaction.setOnClickListener(v -> showDialogTambahTransaksi());

        return view;
    }

    // --- FUNGSI: Mengambil Nama User untuk Sapaan ---
    private void loadUserName() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String fullName = user.getDisplayName();
            if (fullName != null && !fullName.isEmpty()) {
                tvName.setText(fullName);
            } else {
                // Fallback jika nama tidak ada, coba ambil bagian depan email
                String email = user.getEmail();
                if (email != null) {
                    String nameFromEmail = email.split("@")[0];
                    tvName.setText(nameFromEmail);
                } else {
                    tvName.setText("Pengguna");
                }
            }
        }
    }

    // --- FUNGSI: Mengambil Data Transaksi dari Firebase ---
    private void loadDataFirebase() {
        if (databaseReference == null) return;

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Cek agar tidak crash jika fragment sudah ditutup
                if (getContext() == null) return;

                transaksiList.clear();
                layoutListTransaksi.removeAllViews(); // Bersihkan list lama agar tidak duplikat

                double totalMasuk = 0;
                double totalKeluar = 0;

                // Loop setiap data transaksi
                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Transaksi trx = data.getValue(Transaksi.class);
                        if (trx != null) {
                            // Tambahkan ke list di urutan paling atas (index 0) agar yang baru muncul di atas
                            transaksiList.add(0, trx);

                            // Hitung Total Pemasukan & Pengeluaran
                            if ("Pemasukan".equals(trx.getTipe())) {
                                totalMasuk += trx.getJumlah();
                            } else {
                                totalKeluar += trx.getJumlah();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Update UI Ringkasan Saldo
                updateRingkasanUI(totalMasuk, totalKeluar);

                // Tampilkan List Transaksi ke Layar
                for (Transaksi trx : transaksiList) {
                    addCardToLayout(trx);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateRingkasanUI(double masuk, double keluar) {
        double saldo = masuk - keluar;
        tvTotalSaldo.setText(formatRupiah(saldo));
        tvTotalPemasukan.setText("+ " + formatRupiah(masuk));
        tvTotalPengeluaran.setText("- " + formatRupiah(keluar));
    }

    // --- FUNGSI: Menambahkan Card Item ke Layout Secara Dinamis ---
    private void addCardToLayout(Transaksi trx) {
        if (getContext() == null) return;

        // Inflate layout item_transaksi_card.xml
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_transaksi_card, layoutListTransaksi, false);

        TextView tvInisial = cardView.findViewById(R.id.tvInisial);
        CardView cvInisial = cardView.findViewById(R.id.cvInisial);
        TextView tvDeskripsi = cardView.findViewById(R.id.tvDeskripsi);
        TextView tvTanggal = cardView.findViewById(R.id.tvTanggal);
        TextView tvNominal = cardView.findViewById(R.id.tvNominal);

        // Set Data ke View
        tvDeskripsi.setText(trx.getDeskripsi());
        tvTanggal.setText(trx.getTanggal());
        tvInisial.setText(trx.getKategori()); // Menampilkan inisial/ikon kategori

        // Styling Warna Berdasarkan Tipe Transaksi
        if ("Pemasukan".equals(trx.getTipe())) {
            tvNominal.setText("+ " + formatRupiah(trx.getJumlah()));
            tvNominal.setTextColor(ContextCompat.getColor(getContext(), R.color.income_green));
            cvInisial.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.income_green_bg));
            tvInisial.setTextColor(ContextCompat.getColor(getContext(), R.color.income_green));
        } else {
            tvNominal.setText("- " + formatRupiah(trx.getJumlah()));
            tvNominal.setTextColor(ContextCompat.getColor(getContext(), R.color.expense_red));
            cvInisial.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.expense_red_bg));
            tvInisial.setTextColor(ContextCompat.getColor(getContext(), R.color.expense_red));
        }

        // Set Listener Klik Card untuk Detail
        cardView.setOnClickListener(v -> showDetailBottomSheet(trx));

        // Tambahkan view ke container
        layoutListTransaksi.addView(cardView);
    }

    // --- FITUR DETAIL (BOTTOM SHEET) ---
    private void showDetailBottomSheet(Transaksi trx) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View bottomSheetView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_detail_transaksi, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextView tvInisial = bottomSheetView.findViewById(R.id.tvDetailInisial);
        CardView cvInisial = bottomSheetView.findViewById(R.id.cvDetailInisial);
        TextView tvAmount = bottomSheetView.findViewById(R.id.tvDetailAmount);
        TextView tvType = bottomSheetView.findViewById(R.id.tvDetailType);
        TextView tvDesc = bottomSheetView.findViewById(R.id.tvDetailDescription);
        TextView tvDate = bottomSheetView.findViewById(R.id.tvDetailDate);
        Button btnEdit = bottomSheetView.findViewById(R.id.btnDetailEdit);
        Button btnDelete = bottomSheetView.findViewById(R.id.btnDetailDelete);

        tvInisial.setText(trx.getKategori());
        tvDesc.setText(trx.getDeskripsi());
        tvDate.setText(trx.getTanggal());
        tvType.setText(trx.getTipe());

        if ("Pemasukan".equals(trx.getTipe())) {
            tvAmount.setText("+ " + formatRupiah(trx.getJumlah()));
            tvAmount.setTextColor(ContextCompat.getColor(getContext(), R.color.income_green));
            cvInisial.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.income_green_bg));
            tvInisial.setTextColor(ContextCompat.getColor(getContext(), R.color.income_green));
        } else {
            tvAmount.setText("- " + formatRupiah(trx.getJumlah()));
            tvAmount.setTextColor(ContextCompat.getColor(getContext(), R.color.expense_red));
            cvInisial.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.expense_red_bg));
            tvInisial.setTextColor(ContextCompat.getColor(getContext(), R.color.expense_red));
        }

        // Tombol Edit
        btnEdit.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showDialogEditTransaksi(trx);
        });

        // Tombol Hapus
        btnDelete.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showDialogHapus(trx.getId());
        });

        bottomSheetDialog.show();
    }

    // --- FITUR TAMBAH (DIALOG) ---
    private void showDialogTambahTransaksi() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_simple, null);
        builder.setView(dialogView);

        EditText etDeskripsi = dialogView.findViewById(R.id.etDeskripsi);
        EditText etJumlah = dialogView.findViewById(R.id.etJumlah);
        EditText etKategori = dialogView.findViewById(R.id.etKategori);
        RadioGroup rgTipe = dialogView.findViewById(R.id.rgTipe);
        Button btnSimpan = dialogView.findViewById(R.id.btnSimpan);

        AlertDialog dialog = builder.create();

        btnSimpan.setOnClickListener(v -> {
            String deskripsi = etDeskripsi.getText().toString();
            String strJumlah = etJumlah.getText().toString();
            String kategori = etKategori.getText().toString(); // Ambil dari input inisial (1 huruf)

            if (TextUtils.isEmpty(deskripsi) || TextUtils.isEmpty(strJumlah)) {
                Toast.makeText(getContext(), "Deskripsi dan Jumlah tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = rgTipe.getCheckedRadioButtonId();
            String tipe = (selectedId == R.id.rbPemasukan) ? "Pemasukan" : "Pengeluaran";
            double jumlah = Double.parseDouble(strJumlah);
            String tanggal = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());

            // Generate ID Unik
            String id = databaseReference.push().getKey();

            // Buat objek Transaksi baru
            // Kategori diambil 1 huruf kapital, jika kosong pakai "?"
            String inisialKategori = kategori.isEmpty() ? "?" : kategori.substring(0,1).toUpperCase();

            Transaksi transaksi = new Transaksi(id, deskripsi, jumlah, tipe, tanggal, inisialKategori);

            if (id != null) {
                databaseReference.child(id).setValue(transaksi)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Berhasil disimpan", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Gagal menyimpan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
        dialog.show();
    }

    // --- FITUR EDIT (DIALOG) ---
    private void showDialogEditTransaksi(Transaksi existingTrx) {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_simple, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etDeskripsi = dialogView.findViewById(R.id.etDeskripsi);
        EditText etJumlah = dialogView.findViewById(R.id.etJumlah);
        EditText etKategori = dialogView.findViewById(R.id.etKategori);
        RadioGroup rgTipe = dialogView.findViewById(R.id.rgTipe);
        RadioButton rbMasuk = dialogView.findViewById(R.id.rbPemasukan);
        RadioButton rbKeluar = dialogView.findViewById(R.id.rbPengeluaran);
        Button btnSimpan = dialogView.findViewById(R.id.btnSimpan);

        tvTitle.setText("Edit Transaksi");
        btnSimpan.setText("UPDATE");

        // Isi data lama ke form
        etDeskripsi.setText(existingTrx.getDeskripsi());
        etJumlah.setText(String.valueOf((long) existingTrx.getJumlah())); // Tampilkan sebagai integer agar rapi
        etKategori.setText(existingTrx.getKategori());

        if ("Pemasukan".equals(existingTrx.getTipe())) {
            rbMasuk.setChecked(true);
        } else {
            rbKeluar.setChecked(true);
        }

        AlertDialog dialog = builder.create();

        btnSimpan.setOnClickListener(v -> {
            String deskripsi = etDeskripsi.getText().toString();
            String strJumlah = etJumlah.getText().toString();
            String kategori = etKategori.getText().toString();

            if (TextUtils.isEmpty(deskripsi) || TextUtils.isEmpty(strJumlah)) return;

            int selectedId = rgTipe.getCheckedRadioButtonId();
            String tipe = (selectedId == R.id.rbPemasukan) ? "Pemasukan" : "Pengeluaran";
            double jumlah = Double.parseDouble(strJumlah);
            String tanggal = existingTrx.getTanggal(); // Tanggal tetap sama

            String inisialKategori = kategori.isEmpty() ? "?" : kategori.substring(0,1).toUpperCase();

            Transaksi updatedTransaksi = new Transaksi(existingTrx.getId(), deskripsi, jumlah, tipe, tanggal, inisialKategori);

            // Update ke Firebase
            databaseReference.child(existingTrx.getId()).setValue(updatedTransaksi)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Berhasil diupdate", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });
        dialog.show();
    }

    // --- FITUR HAPUS ---
    private void showDialogHapus(String id) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Hapus Transaksi")
                .setMessage("Apakah Anda yakin ingin menghapus data ini?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    databaseReference.child(id).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Data Terhapus", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // --- Helper Format Rupiah ---
    private String formatRupiah(double number) {
        NumberFormat formatKurensi = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return formatKurensi.format(number);
    }
}