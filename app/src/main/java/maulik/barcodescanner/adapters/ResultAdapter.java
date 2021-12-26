package maulik.barcodescanner.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import maulik.barcodescanner.R;
import maulik.barcodescanner.modals.BarcodeData;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ResultViewHolder> {

    private Context mContext;
    private List<BarcodeData> barcodeDataList;

    public ResultAdapter(Context mContext) {
        barcodeDataList = new ArrayList<>();
        this.mContext = mContext;
        this.barcodeDataList = barcodeDataList;
    }

    @NonNull
    @Override
    public ResultAdapter.ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.result_itemview, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultAdapter.ResultViewHolder holder, int position) {
        BarcodeData data = barcodeDataList.get(position);
        holder.barcodeResult.setText(data.getResult());
        holder.barcodeAddress.setText(data.getAddress());
        holder.gps.setText(data.getLat() + "," + data.getLon());
        CharSequence relativeDate = DateUtils.getRelativeDateTimeString(mContext, data.getTime() , DateUtils.SECOND_IN_MILLIS , DateUtils.WEEK_IN_MILLIS , DateUtils.FORMAT_ABBREV_RELATIVE);
        holder.barcodeTime.setText(relativeDate);

    }

    @Override
    public int getItemCount() {
        return barcodeDataList.size();
    }

    protected class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView barcodeResult;
        TextView barcodeAddress;
        TextView gps;
        TextView barcodeTime;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            barcodeResult = itemView.findViewById(R.id.barcode_result);
            barcodeAddress = itemView.findViewById(R.id.barcode_address);
            gps = itemView.findViewById(R.id.gps_tv);
            barcodeTime = itemView.findViewById(R.id.barcode_time);
        }
    }

    public void updateData(List<BarcodeData> barcodeDataList){
        this.barcodeDataList = barcodeDataList;
        notifyDataSetChanged();
    }
}
