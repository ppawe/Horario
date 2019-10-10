package hft.wiinf.de.horario.view;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.view.InvitationFragment.OnListFragmentInteractionListener;

/*
RecyclerViewAdapter receives a list object and generates a view for each invitation object in the list
Each view has an onClickListener that calls TabActivity.onListFragmentInteraction with the invitation as the parameter
 */
public class MyInvitationRecyclerViewAdapter extends RecyclerView.Adapter<MyInvitationRecyclerViewAdapter.ViewHolder> {

    private final List<Event> mValues;
    private final OnListFragmentInteractionListener mListener;
    private final boolean isListEmpty;

    public MyInvitationRecyclerViewAdapter(List<Event> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        if(items.size() == 0){
            mValues.add(new Event());
            isListEmpty = true;
        }else {
            isListEmpty = false;
        }
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        if(!isListEmpty) {
            holder.mContentView.setText(mValues.get(position).getShortTitle());
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        mListener.onListFragmentInteraction(holder.mItem);
                    }
                }
            });
        }else{
            holder.mContentView.setText(R.string.no_new_invitations);
        }


    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mContentView;
        public Event mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = (TextView) view.findViewById(android.R.id.text1);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}