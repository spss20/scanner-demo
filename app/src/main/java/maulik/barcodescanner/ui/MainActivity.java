package maulik.barcodescanner.ui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.evrencoskun.tableview.TableView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kotlin.jvm.internal.Intrinsics;
import maulik.barcodescanner.R;
import maulik.barcodescanner.adapters.MyTableViewAdapter;
import maulik.barcodescanner.databinding.ActivityMainBinding;
import maulik.barcodescanner.listeners.TableViewListener;
import maulik.barcodescanner.modals.BarcodeData;
import maulik.barcodescanner.modals.Cell;
import maulik.barcodescanner.modals.ColumnHeader;
import maulik.barcodescanner.modals.RowHeader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private final int cameraPermissionRequestCode = 1;
    private BarcodeScanningActivity.ScannerSDK selectedScanningSDK;
    private ActivityMainBinding binding;
    @NotNull
    private String eventName;
    private BroadcastReceiver eventReceiver;
    private String lastResult = "";
    private FirebaseFirestore firebaseFirestore;
    private FusedLocationProviderClient fusedLocationClient;
    private Location location;
    private String address;

    TableView tableView;
    private List<RowHeader> mRowHeaderList;
    private List<ColumnHeader> mColumnHeaderList;
    private List<List<Cell>> mCellList;
    MyTableViewAdapter adapter;

    public MainActivity() {
        this.selectedScanningSDK = BarcodeScanningActivity.ScannerSDK.ZXING;
        this.firebaseFirestore = FirebaseFirestore.getInstance();
        this.eventName = "scanner_data";
        mRowHeaderList = new ArrayList<>();
        mColumnHeaderList = new ArrayList<>();
        mCellList = new ArrayList<>();
        this.eventReceiver = (BroadcastReceiver) (new BroadcastReceiver() {
            public void onReceive(@Nullable Context context, @Nullable Intent intent) {
                if (intent != null) {
                    String result = intent.getStringExtra("data");
                    if (lastResult.equals(result))
                        return;
                    lastResult = result;

                    BarcodeData data = new BarcodeData(result, "NIL", System.currentTimeMillis(), "NIL", "NIL");
                    if (location != null) {
                        data.setLat(String.valueOf(location.getLatitude()));
                        data.setLon(String.valueOf(location.getLongitude()));
                    }
                    if (address != null)
                        data.setAddress(address);

                    firebaseFirestore.collection("barcodes")
                            .add(data)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(@NonNull DocumentReference documentReference) {
                                    Toast.makeText(MainActivity.this, "Successfully updated to server", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Failed to update to the server", Toast.LENGTH_SHORT).show();
                                }
                            });
                }

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from((Context) this));
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setTitle("Galler 2D Scanner");
        tableView = findViewById(R.id.content_container);
        tableView.setTableViewListener(new TableViewListener(tableView));

        mColumnHeaderList.add(new ColumnHeader("Result"));
        mColumnHeaderList.add((new ColumnHeader("Date")));
        mColumnHeaderList.add((new ColumnHeader("Address")));
        mColumnHeaderList.add((new ColumnHeader("GPS")));

        adapter = new MyTableViewAdapter();
        tableView.setAdapter(adapter);

        firebaseFirestore.collection("barcodes")
                .orderBy("time", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@androidx.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @androidx.annotation.Nullable FirebaseFirestoreException e) {
                        Log.v(TAG, "Barcode list successfully fetched");
                        mCellList.clear();
                        mRowHeaderList.clear();
                        if (queryDocumentSnapshots != null) {
                            List<DocumentSnapshot> documentList = queryDocumentSnapshots.getDocuments();
                            for (int i = 0; i < documentList.size(); i++) {
                                BarcodeData barcodeData = documentList.get(i).toObject(BarcodeData.class);
                                List<Cell> cellList = new ArrayList<>();
                                cellList.add(new Cell(barcodeData.getResult()));

                                Date date = new Date(barcodeData.getTime());
                                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss");
                                String dateTimeFormatted = sf.format(date);

                                cellList.add(new Cell(dateTimeFormatted));
                                cellList.add(new Cell(barcodeData.getAddress()));
                                cellList.add(new Cell(barcodeData.getLat() + "," + barcodeData.getLon()));
                                mCellList.add(cellList);
                                mRowHeaderList.add(new RowHeader(String.valueOf(i + 1)));
                            }

                            adapter.setAllItems(mColumnHeaderList, mRowHeaderList, mCellList);
                            tableView.setColumnWidth(0 , 400);
                            tableView.setColumnWidth(2 , 300);

                        }
                    }
                });

        registerEventReceiver();
        startScanning();

        binding.floatingScanNow.setOnClickListener((View.OnClickListener) (it -> startScanning()));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                MainActivity.this.location = location;
                                // Logic to handle location object
                                Geocoder geocoder;
                                List<Address> addresses;
                                geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                try {
                                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                                    String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                                    MainActivity.this.address = address;
//                                String city = addresses.get(0).getLocality();
//                                String state = addresses.get(0).getAdminArea();
//                                String country = addresses.get(0).getCountryName();
//                                String postalCode = addresses.get(0).getPostalCode();
//                                String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL

                                    Log.v(TAG, address);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }


                        }
                    });
            return;
        }

    }

    private final void startScanning() {
        if (ContextCompat.checkSelfPermission((Context) this, "android.permission.CAMERA") == 0) {
            this.openCameraWithScanner();
        } else {
            ActivityCompat.requestPermissions((Activity) this, new String[]{"android.permission.CAMERA"}, this.cameraPermissionRequestCode);
        }

    }

    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        Intrinsics.checkNotNullParameter(permissions, "permissions");
        Intrinsics.checkNotNullParameter(grantResults, "grantResults");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == this.cameraPermissionRequestCode) {
            boolean var5 = false;
            boolean var7 = false;
            if (grantResults.length != 0) {
                if (grantResults[0] == 0) {
                    this.openCameraWithScanner();
                } else if (!ActivityCompat.shouldShowRequestPermissionRationale((Activity) this, "android.permission.CAMERA")) {
                    Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                    Uri var10000 = Uri.fromParts("package", this.getPackageName(), (String) null);
                    Intrinsics.checkNotNullExpressionValue(var10000, "Uri.fromParts(\"package\", packageName, null)");
                    Uri uri = var10000;
                    intent.setData(uri);
                    this.startActivityForResult(intent, this.cameraPermissionRequestCode);
                }
            }
        }

    }

    private final void openCameraWithScanner() {
        BarcodeScanningActivity.Companion.start((Context) this, this.selectedScanningSDK);
    }

    protected void onDestroy() {
        super.onDestroy();
    }

    private final void registerEventReceiver() {
        IntentFilter eventFilter = new IntentFilter();
        eventFilter.addAction(this.eventName);
        this.registerReceiver(this.eventReceiver, eventFilter);
    }


    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == this.cameraPermissionRequestCode && ContextCompat.checkSelfPermission((Context) this, "android.permission.CAMERA") == 0) {
            this.openCameraWithScanner();
        }

    }
}