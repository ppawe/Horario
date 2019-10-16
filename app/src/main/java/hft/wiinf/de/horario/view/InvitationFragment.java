package hft.wiinf.de.horario.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import hft.wiinf.de.horario.R;
import hft.wiinf.de.horario.controller.EventPersonController;
import hft.wiinf.de.horario.controller.PersonController;
import hft.wiinf.de.horario.model.Event;


/**
 * A fragment representing a list of {@link Event}s that the user is invited to.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class InvitationFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public InvitationFragment() {
    }

    @SuppressWarnings("unused")
    public static InvitationFragment newInstance(int columnCount) {
        InvitationFragment fragment = new InvitationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * gets the column count argument from the saved instance state
     *
     * @param savedInstanceState the state to which the fragment should be restored after a system event
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    /**
     * Inflates the layout fragment_invitation_list.xml and sets a {@link MyInvitationRecyclerViewAdapter} with
     * {@link Event}s the user has been invited to for the RecyclerView in it
     *
     * @param inflater           LayoutInflater for inflating the layout into views
     * @param container          the parent view of the fragment
     * @param savedInstanceState the saved state of the fragment from before some system event changed it
     * @return the inflated layout's view
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_invitation_list, container, false);
        View view = parentView.findViewById(R.id.invitationlist);
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            EventPersonController.deleteExpiredPendingEventsForPerson(PersonController.getPersonWhoIam());
            recyclerView.setAdapter(new MyInvitationRecyclerViewAdapter(EventPersonController.getAllInvitedEventsForPersonWithoutSerials(PersonController.getPersonWhoIam()), mListener));
        }
        return parentView;
    }


    /**
     * this method is called when the view created in onCreateView() is attached to the root view
     * checks if the context is an instance of OnListFragmentInteractionListener and throws an exception if it is not
     * else it saves the context in a variable
     * @param context the parent activity of the fragment
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    /**
     * removes the saved context if the fragment is detached from the parent activity
     * do not ask me what this means but it has something to do with the fragment lifecycle
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(Event item);
    }
}
