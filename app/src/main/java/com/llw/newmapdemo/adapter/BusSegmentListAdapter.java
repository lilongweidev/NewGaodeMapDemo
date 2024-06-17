package com.llw.newmapdemo.adapter;

import static com.amap.api.maps.model.BitmapDescriptorFactory.getContext;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.route.RailwayStationItem;
import com.llw.newmapdemo.R;
import com.llw.newmapdemo.databinding.ItemSegmentBinding;
import com.llw.newmapdemo.utils.SchemeBusStep;

import java.util.List;

/**
 * 公交段列表适配器
 */
public class BusSegmentListAdapter extends RecyclerView.Adapter<BusSegmentListAdapter.ViewHolder> {

    private List<SchemeBusStep> mItemList;

    public BusSegmentListAdapter(List<SchemeBusStep> data) {
        this.mItemList = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSegmentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SchemeBusStep item = mItemList.get(position);
        if (position == 0) {
            holder.binding.busDirIcon.setImageResource(R.drawable.dir_start);
            holder.binding.busLineName.setText("出发");
            holder.binding.busDirIconUp.setVisibility(View.INVISIBLE);
            holder.binding.busDirIconDown.setVisibility(View.VISIBLE);
            holder.binding.busSegSplitLine.setVisibility(View.INVISIBLE);
        } else if (position == mItemList.size() - 1) {
            holder.binding.busDirIcon.setImageResource(R.drawable.dir_end);
            holder.binding.busLineName.setText("到达终点");
            holder.binding.busDirIconUp.setVisibility(View.VISIBLE);
            holder.binding.busDirIconDown.setVisibility(View.INVISIBLE);
        } else {
            if (item.isWalk() && item.getWalk() != null && item.getWalk().getDistance() > 0) {
                holder.binding.busDirIcon.setImageResource(R.drawable.dir13);
                holder.binding.busDirIconUp.setVisibility(View.VISIBLE);
                holder.binding.busDirIconDown.setVisibility(View.VISIBLE);
                holder.binding.busLineName.setText("步行"
                        + (int) item.getWalk().getDistance() + "米");
                holder.binding.busStationNum.setVisibility(View.GONE);
                holder.binding.busExpandImage.setVisibility(View.GONE);
            } else if (item.isBus() && item.getBusLines().size() > 0) {
                holder.binding.busDirIcon.setImageResource(R.drawable.dir14);
                holder.binding.busDirIconUp.setVisibility(View.VISIBLE);
                holder.binding.busDirIconDown.setVisibility(View.VISIBLE);
                holder.binding.busLineName.setText(item.getBusLines().get(0).getBusLineName());
                holder.binding.busStationNum.setVisibility(View.VISIBLE);
                holder.binding.busStationNum
                        .setText((item.getBusLines().get(0).getPassStationNum() + 1) + "站");
                holder.binding.busExpandImage.setVisibility(View.VISIBLE);
            } else if (item.isRailway() && item.getRailway() != null) {
                holder.binding.busDirIcon.setImageResource(R.drawable.dir16);
                holder.binding.busDirIconUp.setVisibility(View.VISIBLE);
                holder.binding.busDirIconDown.setVisibility(View.VISIBLE);
                holder.binding.busLineName.setText(item.getRailway().getName());
                holder.binding.busStationNum.setVisibility(View.VISIBLE);
                holder.binding.busStationNum
                        .setText((item.getRailway().getViastops().size() + 1) + "站");
                holder.binding.busExpandImage.setVisibility(View.VISIBLE);
            } else if (item.isTaxi() && item.getTaxi() != null) {
                holder.binding.busDirIcon.setImageResource(R.drawable.dir14);
                holder.binding.busDirIconUp.setVisibility(View.VISIBLE);
                holder.binding.busDirIconDown.setVisibility(View.VISIBLE);
                holder.binding.busLineName.setText("打车到终点");
                holder.binding.busStationNum.setVisibility(View.GONE);
                holder.binding.busExpandImage.setVisibility(View.GONE);
            }
        }

        holder.binding.busItem.setOnClickListener(v -> {
            if (item.isBus()) {//公交
                if (!item.isArrowExpend()) {
                    item.setArrowExpend(true);
                    holder.binding.busExpandImage.setImageResource(R.drawable.up);
                    addBusStation(item.getBusLine().getDepartureBusStation(), holder.binding.expandContent);
                    for (BusStationItem station : item.getBusLine()
                            .getPassStations()) {
                        addBusStation(station, holder.binding.expandContent);
                    }
                    addBusStation(item.getBusLine().getArrivalBusStation(), holder.binding.expandContent);

                } else {
                    item.setArrowExpend(false);
                    holder.binding.busExpandImage.setImageResource(R.drawable.down);
                    holder.binding.expandContent.removeAllViews();
                }
            } else if (item.isRailway()) {//火车
                if (!item.isArrowExpend()) {
                    item.setArrowExpend(true);
                    holder.binding.busExpandImage.setImageResource(R.drawable.up);
                    addRailwayStation(item.getRailway().getDeparturestop(), holder.binding.expandContent);
                    for (RailwayStationItem station : item.getRailway().getViastops()) {
                        addRailwayStation(station, holder.binding.expandContent);
                    }
                    addRailwayStation(item.getRailway().getArrivalstop(), holder.binding.expandContent);

                } else {
                    item.setArrowExpend(false);
                    holder.binding.busExpandImage.setImageResource(R.drawable.down);
                    holder.binding.expandContent.removeAllViews();
                }
            }
        });
    }

    /**
     * 添加公交车站
     * @param station
     * @param expandContent
     */
    private void addBusStation(BusStationItem station, LinearLayout expandContent) {
        LinearLayout ll = (LinearLayout) View.inflate(getContext(),
                R.layout.item_segment_ex, null);
        TextView tv = ll.findViewById(R.id.bus_line_station_name);
        tv.setText(station.getBusStationName());
        expandContent.addView(ll);
    }

    /**
     * 添加火车站
     * @param station
     * @param expandContent
     */
    private void addRailwayStation(RailwayStationItem station, LinearLayout expandContent) {
        LinearLayout ll = (LinearLayout) View.inflate(getContext(),
                R.layout.item_segment_ex, null);
        TextView tv = ll
                .findViewById(R.id.bus_line_station_name);
        tv.setText(station.getName() + " " + getRailwayTime(station.getTime()));
        expandContent.addView(ll);
    }

    /**
     * 获取铁路时间
     * @param time
     * @return
     */
    public static String getRailwayTime(String time) {
        return time.substring(0, 2) + ":" + time.substring(2, time.length());
    }

    @Override
    public int getItemCount() {
        return mItemList == null || mItemList.isEmpty() ? 0 : mItemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemSegmentBinding binding;

        public ViewHolder(ItemSegmentBinding itemView) {
            super(itemView.getRoot());
            this.binding = itemView;
        }
    }
}
