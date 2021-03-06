package com.example.capstonee.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.capstonee.Chat.AES256Util;
import com.example.capstonee.Model.Chat;
import com.example.capstonee.Model.Infomation;
import com.example.capstonee.Model.Login;
import com.example.capstonee.Model.User;
import com.example.capstonee.R;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    //keyFlag는 항상 트루 그러나, 캡차 5번 실패시 false로 변환됨.
    public static boolean keyFlag = true;
    // chat 모음
    ArrayList<Chat> chatList = new ArrayList<>();
    Context context;
    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;
    private final String myID = Login.getUserID();

    public MessageAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = null;
        ViewHolder viewHolder = null;
        if (i == MSG_TYPE_RIGHT) {
            view = LayoutInflater.from(context).inflate(R.layout.chat_item_right, viewGroup, false);
            return new ViewHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.chat_item_left, viewGroup, false);
            TextView userName_textView = (TextView) view.findViewById(R.id.user_id_text_view);
            return new ViewHolder(view, userName_textView);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        Chat chat = chatList.get(position);
//        viewHolder.show_message.setText(chat.getMessage());

        viewHolder.onBind(chat);
    }

    public void add(Chat chat) {
        chatList.add(chat);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        // message sender
        String sender = chatList.get(position).getSender();
        if (sender.equals(myID))
            return MSG_TYPE_RIGHT;
        return MSG_TYPE_LEFT;


    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView profile_image;
        private Chat chat;
        private TextView message_textView, userName_textView;
        Set<String> readerList;
        private String message, readersText;

        public ViewHolder(View itemView) {
            super(itemView);

            profile_image = (CircleImageView) itemView.findViewById(R.id.profile_image);
            message_textView = itemView.findViewById(R.id.show_message_text_view);

            // 꾹 누르면 읽은 사람 표시
            show_reader(itemView);
        }

        // 왼쪽 이름 추가하기 위함
        public ViewHolder(final View itemView, TextView userName_textView) {
            super(itemView);

            profile_image = (CircleImageView) itemView.findViewById(R.id.profile_image);
            message_textView = itemView.findViewById(R.id.show_message_text_view);
            this.userName_textView = userName_textView;

            show_reader(itemView);

        }

        // 읽은 사람 보여줘!
        private void show_reader(View itemView) {

            // 꾹 누르면 읽은 사람 표시
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    final AlertDialog.Builder ad = new AlertDialog.Builder(context);

                    ad.setTitle("읽은 사람");       // 제목 설정

                    readersText = "";

                    // chat의 reader 의
                    readerList = chat.getReader().keySet();

                    Log.e("s", readerList.size() + "");

                    if (readerList.size() == 0 || readerList.isEmpty()) {
                        readersText = "읽은 사람이 없습니다.";

                        ad.setMessage(readersText);   // 내용 설정

                        // 창 띄우기
                        ad.show();

                    } else

                        Infomation.getDatabase("User").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (String id : readerList) {
                                    User user = dataSnapshot.child(id).getValue(User.class);
                                    readersText += user.getName() + "\n";
                                }

                                ad.setMessage(readersText);   // 내용 설정

                                // 창 띄우기
                                ad.show();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    return false;
                }
            });

        }


        void onBind(final Chat chat) {
            this.chat = chat;
            if (profile_image != null) {
                Infomation.getDatabase("User").child(chat.getSender()).child("profileUri").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue().toString().equals(""))
                            profile_image.setImageResource(R.drawable.default_profile);
                        else
                            Picasso.with(context).load(dataSnapshot.getValue().toString()).fit().into(profile_image);
                        ;

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
            try {
                AES256Util aes = new AES256Util();
                message = chat.getMessage();
                if (keyFlag == true) {   //키 플래그는 항상 트루. 그러나 만약, 캡차에서 5번 틀릴시에는 false로 바꾸게 했다.
                    message = aes.decrypt(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 이름 설정 가능 하면 이름 넣어준다. -> 왼쪽 상대라는 소리지!
            // 근데 이거 관계형 디비 아닌게 극형이네;;
            // chat 에 이름으로 넣어버릴까? -> 이거 읽으신 분은 카톡으로 의견좀 ㅎㅎ
            if (userName_textView != null) {
                Infomation.getDatabase("User").child(chat.getSender()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        // 이름으로 추가
                        userName_textView.setText(user.getName());
                        if (chat.getUri().equals(""))
                            message_textView.setText(message);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            } else {
                if (chat.getUri().equals(""))
                    //message_textView.setText(chat.getMessage());
                    message_textView.setText(message);
            }

        }

    }

}