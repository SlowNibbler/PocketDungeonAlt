/**
 * This class handles login activity from a user.
 *
 * @author: Meng Yang
 */
package edu.tacoma.wa.pocketdungeonalt.authenticate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;


import edu.tacoma.wa.pocketdungeonalt.MainActivity;
import edu.tacoma.wa.pocketdungeonalt.R;
import edu.tacoma.wa.pocketdungeonalt.authenticate.RegisterFragment;
import edu.tacoma.wa.pocketdungeonalt.model.User;

/**
 * Login fragment to login a user account.
 */
public class LoginFragment extends Fragment {

    private LoginFragmentListener mLoginFragmentListener;
    private JSONObject mUserJSON;

    public LoginFragment() {
        // Required empty public constructor
    }

    public interface LoginFragmentListener {
        void login(String email, String pwd);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /** Inflate the layout for this fragment.
         *  Get email and password from user entry.
         */
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        getActivity().setTitle("Sign In");
        mLoginFragmentListener = (LoginFragmentListener) getActivity();
        final EditText emailText = view.findViewById(R.id.email);
        final EditText pwdText = view.findViewById(R.id.password);
        Button loginButton = view.findViewById(R.id.button_login);
        TextView txtRegister = view.findViewById(R.id.text_register);

        /** when user clicks on the login button */
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailText.getText().toString();
                String password = pwdText.getText().toString();
                StringBuilder url = new StringBuilder(getString(R.string.login));

                /** construct a JSONObject to build a formatted message to send. */
                mUserJSON = new JSONObject();
                try {
                    mUserJSON.put(User.EMAIL, email);
                    mUserJSON.put(User.PASSWORD, password);
                    new LoginFragment.LoginAsyncTask().execute(url.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mLoginFragmentListener.login(email, password);
            }
        });

        /** when user clicks on the 'Don't have an account' text, open register screen */
        txtRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterFragment registerFragment = new RegisterFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.sign_in_fragment_id, registerFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        return view;
    }

    /** Send post request to server, check login credentials. */
    private class LoginAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            HttpURLConnection urlConnection = null;
            for (String url : urls) {
                try {
                    URL urlObject = new URL(url);
                    System.out.println("urlL " + url);
                    urlConnection = (HttpURLConnection) urlObject.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setDoOutput(true);
                    System.out.println("1");
                    OutputStreamWriter wr =
                            new OutputStreamWriter(urlConnection.getOutputStream());
                    System.out.println("2");
                    // For Debugging
                    Log.i("LOGIN", mUserJSON.toString());
                    wr.write(mUserJSON.toString());
                    wr.flush();
                    wr.close();
                    System.out.println("3");
                    /** get response from server*/
                    InputStream content = urlConnection.getInputStream();
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    String s = "";
                    while ((s = buffer.readLine()) != null) {
                        response += s;
                    }

                } catch (Exception e) {
                    response = "Unable to login, Reason: "
                            + e.getMessage();
                } finally {
                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
            }
            System.out.println("4");
            return response;
        }

        /** actions after received response from server */
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);

                /** If login successfully, go to main screen.
                 * Get userId and save to SharedPreferences. */
                if (jsonObject.getBoolean("success")) {
                    System.out.println("goodstuff");
                    Toast.makeText(getActivity(),
                            "Login Successfully", Toast.LENGTH_SHORT).show();

                    SharedPreferences mSharedPreferences = getActivity().getSharedPreferences(getString(R.string.LOGIN_PREFS),
                            Context.MODE_PRIVATE);
                    mSharedPreferences.edit()
                            .putInt(getString(R.string.USERID), jsonObject.getInt("memberId"))
                            .commit();

                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                }

                /** If login failed, show error message. */
                else {
                    System.out.println("failed");
                    Toast.makeText(getActivity(), "Login Failed: invalid email/password."
                            , Toast.LENGTH_SHORT).show();
                    Log.e("LOGIN", jsonObject.getString("error"));
                }
            } catch (JSONException e) {
                e.getMessage();
            }
        }
    }
}
