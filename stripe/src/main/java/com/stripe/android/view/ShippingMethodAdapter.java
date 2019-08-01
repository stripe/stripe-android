package com.stripe.android.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.stripe.android.model.ShippingMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that populates a list with shipping methods
 */
final class ShippingMethodAdapter extends RecyclerView.Adapter<ShippingMethodAdapter.ViewHolder> {

    @NonNull private List<ShippingMethod> mShippingMethods = new ArrayList<>();
    private int mSelectedIndex = 0;

    ShippingMethodAdapter() {}

    @Override
    public int getItemCount() {
        return mShippingMethods.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @NonNull
    @Override
    public ShippingMethodAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(new ShippingMethodView(viewGroup.getContext()), this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        holder.setShippingMethod(mShippingMethods.get(i));
        holder.setSelected(i == mSelectedIndex);
    }

    @Nullable
    ShippingMethod getSelectedShippingMethod() {
        return mShippingMethods.get(mSelectedIndex);
    }

    void setShippingMethods(@Nullable List<ShippingMethod> shippingMethods,
                            @Nullable ShippingMethod defaultShippingMethod) {
        if (shippingMethods != null) {
            mShippingMethods = shippingMethods;
        }
        if (defaultShippingMethod == null) {
            mSelectedIndex = 0;
        } else {
            mSelectedIndex = mShippingMethods.indexOf(defaultShippingMethod);
        }
        notifyDataSetChanged();
    }

    void onShippingMethodSelected(int selectedIndex) {
        mSelectedIndex = selectedIndex;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @NonNull private final ShippingMethodView shippingMethodView;

        private ViewHolder(@NonNull final ShippingMethodView shippingMethodView,
                           @NonNull final ShippingMethodAdapter adapter) {
            super(shippingMethodView);
            this.shippingMethodView = shippingMethodView;
            shippingMethodView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adapter.onShippingMethodSelected(getAdapterPosition());
                }
            });
        }

        private void setShippingMethod(@NonNull ShippingMethod shippingMethod) {
            shippingMethodView.setShippingMethod(shippingMethod);
        }

        private void setSelected(boolean selected) {
            shippingMethodView.setSelected(selected);
        }
    }
}
