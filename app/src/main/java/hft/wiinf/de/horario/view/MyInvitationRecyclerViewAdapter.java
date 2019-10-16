package hft.wiinf.de.horario.view;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.model.Event;
import hft.wiinf.de.horario.view.InvitationFragment.OnListFragmentInteractionListener;

/**
 * RecyclerViewAdapter receives a list object and generates a view for each {@link Event} in the list
 * Each view has an onClickListener that calls TabActivity.onListFragmentInteraction with the event as the parameter
 */

public class MyInvitationRecyclerViewAdapter extends RecyclerView.Adapter<MyInvitationRecyclerViewAdapter.ViewHolder> {

    private final List<Event> mValues;
    private final OnListFragmentInteractionListener mListener;
    private final boolean isListEmpty;

    /**
     * instantiates a new MyInvitationRecyclerViewAdapter for a list of {@link Event}s with the specified listener
     *
     * @param items    the list of events that should be displayed in the RecyclerView
     * @param listener the Activity that handles user interactions with the list items
     */
    public MyInvitationRecyclerViewAdapter(List<Event> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        if (items.size() == 0) {
            mValues.add(new Event());
            isListEmpty = true;
        } else {
            isListEmpty = false;
        }
        mListener = listener;
    }

    /**
     * creates a new ViewHolder if the RecyclerView needs another one to display a list item
     * inflates simple_list_item_1 which acts as a container for the list item
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return the ViewHolder that holds a view of the given view type
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    /**
     * gets the Viewholder which should have its content updated and updates it with the information
     * of the {@link Event} at the given position in the Adapter's data set
     * @param holder the ViewHolder that should be updated
     * @param position the position of the Event that should be displayed
     */
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        if (!isListEmpty) {
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
        } else {
            holder.mContentView.setText(R.string.no_new_invitations);
        }


    }

    /**
     *
     * @return the number of {@link Event}s in the Adapter's data set
     */
    @Override
    public int getItemCount() {
        return mValues.size();
    }

    /**
     * class representing an {@link Event} in form of a view
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mContentView;
        Event mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = view.findViewById(android.R.id.text1);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
