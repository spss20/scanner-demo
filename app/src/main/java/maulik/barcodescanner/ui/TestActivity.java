package maulik.barcodescanner.ui;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.evrencoskun.tableview.TableView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import maulik.barcodescanner.R;
import maulik.barcodescanner.adapters.MyTableViewAdapter;
import maulik.barcodescanner.listeners.TableViewListener;
import maulik.barcodescanner.modals.BarcodeData;
import maulik.barcodescanner.modals.Cell;
import maulik.barcodescanner.modals.ColumnHeader;
import maulik.barcodescanner.modals.RowHeader;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";
    TableView tableView;
    private List<RowHeader> mRowHeaderList;
    private List<ColumnHeader> mColumnHeaderList;
    private List<List<Cell>> mCellList;

    FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
    MyTableViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        setTitle("Galler 2D Scanner");
        tableView = findViewById(R.id.content_container);
        tableView.setTableViewListener(new TableViewListener(tableView));

        mRowHeaderList = new ArrayList<>();
        mColumnHeaderList = new ArrayList<>();
        mCellList = new ArrayList<>();

//        mRowHeaderList.add(new RowHeader("Row 1"));
//        mRowHeaderList.add((new RowHeader("Row 2")));
        ColumnHeader columnHeader = new ColumnHeader("Result");
        mColumnHeaderList.add(new ColumnHeader("Result"));
        mColumnHeaderList.add((new ColumnHeader("Date Scanned")));
        mColumnHeaderList.add((new ColumnHeader("Address")));
        mColumnHeaderList.add((new ColumnHeader("GPS")));

        firebaseFirestore.collection("barcodes")
                .orderBy("time", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@androidx.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @androidx.annotation.Nullable FirebaseFirestoreException e) {
                        Log.v(TAG, "Barcode lsit successfully fetched");
                        if (queryDocumentSnapshots != null) {
                            List<DocumentSnapshot> documentList = queryDocumentSnapshots.getDocuments();
//                            List<BarcodeData> barcodeDataList = new ArrayList<>();
                            for (int i = 0; i < documentList.size(); i++) {
                                BarcodeData barcodeData = documentList.get(i).toObject(BarcodeData.class);
                                List<Cell> cellList = new ArrayList<>();
                                cellList.add(new Cell(barcodeData.getResult()));

                                Date date = new Date(barcodeData.getTime());
                                SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String dateTimeFormatted = sf.format(date);

                                cellList.add(new Cell(dateTimeFormatted));
                                cellList.add(new Cell(barcodeData.getAddress()));
                                cellList.add(new Cell(barcodeData.getLat() + "," + barcodeData.getLon()));
                                mCellList.add(cellList);
                                mRowHeaderList.add(new RowHeader(String.valueOf(i + 1)));
                            }

                            adapter.setAllItems(mColumnHeaderList, mRowHeaderList, mCellList);
//                            adapter.updateData(barcodeDataList);
                        }
                    }
                });

//        List<Cell> cellList = new ArrayList<>();
//        cellList.add(new Cell("12"));
//        cellList.add(new Cell("34"));
//        cellList.add(new Cell("56"));
//        cellList.add(new Cell("78"));
//
//        mCellList.add(cellList);
//        mCellList.add(cellList1);

        // Create our custom TableView Adapter
        adapter = new MyTableViewAdapter();

        // Set this adapter to the our TableView
        tableView.setAdapter(adapter);

        // Let's set data's of the TableView on the Adapter

    }
}