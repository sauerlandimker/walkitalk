package de.pixart.messenger.ui.adapter;

import android.content.SharedPreferences;
import androidx.databinding.DataBindingUtil;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wefika.flowlayout.FlowLayout;

import java.util.List;

import de.pixart.messenger.R;
import de.pixart.messenger.databinding.ContactBinding;
import de.pixart.messenger.entities.ListItem;
import de.pixart.messenger.ui.SettingsActivity;
import de.pixart.messenger.ui.XmppActivity;
import de.pixart.messenger.ui.util.AvatarWorkerTask;
import de.pixart.messenger.ui.util.StyledAttributes;
import de.pixart.messenger.utils.EmojiWrapper;
import de.pixart.messenger.utils.IrregularUnicodeDetector;
import rocks.xmpp.addr.Jid;

public class ListItemAdapter extends ArrayAdapter<ListItem> {

    private static final float INACTIVE_ALPHA = 0.4684f;
    private static final float ACTIVE_ALPHA = 1.0f;
    protected XmppActivity activity;
    protected boolean showDynamicTags = false;
    private OnTagClickedListener mOnTagClickedListener = null;
    protected int color = 0;
    protected boolean offline = false;
    private View.OnClickListener onTagTvClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view instanceof TextView && mOnTagClickedListener != null) {
                TextView tv = (TextView) view;
                final String tag = tv.getText().toString();
                mOnTagClickedListener.onTagClicked(tag);
            }
        }
    };

    public ListItemAdapter(XmppActivity activity, List<ListItem> objects) {
        super(activity, 0, objects);
        this.activity = activity;
    }

    public void refreshSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        this.showDynamicTags = preferences.getBoolean(SettingsActivity.SHOW_DYNAMIC_TAGS, false);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = activity.getLayoutInflater();
        ListItem item = getItem(position);
        ViewHolder viewHolder;
        if (view == null) {
            ContactBinding binding = DataBindingUtil.inflate(inflater, R.layout.contact, parent, false);
            viewHolder = ViewHolder.get(binding);
            view = binding.getRoot();
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        view.setBackground(StyledAttributes.getDrawable(view.getContext(), R.attr.list_item_background));
        List<ListItem.Tag> tags = item.getTags(activity);
        if (tags.size() == 0 || !this.showDynamicTags) {
            viewHolder.tags.setVisibility(View.GONE);
        } else {
            viewHolder.tags.setVisibility(View.VISIBLE);
            viewHolder.tags.removeAllViewsInLayout();
            for (ListItem.Tag tag : tags) {
                TextView tv = (TextView) inflater.inflate(R.layout.list_item_tag, viewHolder.tags, false);
                tv.setText(tag.getName());
                tv.setBackgroundColor(tag.getColor());
                tv.setOnClickListener(this.onTagTvClick);
                viewHolder.tags.addView(tv);
            }
        }
        final Jid jid = item.getJid();
        if (jid != null) {
            viewHolder.jid.setVisibility(View.VISIBLE);
            viewHolder.jid.setText(IrregularUnicodeDetector.style(activity, jid));
        } else {
            viewHolder.jid.setVisibility(View.GONE);
        }
        if (activity.xmppConnectionService.multipleAccounts() && activity.xmppConnectionService.showOwnAccounts()) {
            viewHolder.account.setVisibility(View.VISIBLE);
            viewHolder.account.setText(item.getAccount().getJid().asBareJid());
        } else {
            viewHolder.account.setVisibility(View.GONE);
        }
        viewHolder.name.setText(EmojiWrapper.transform(item.getDisplayName()));
        if (tags.size() != 0) {
            for (ListItem.Tag tag : tags) {
                offline = tag.getOffline() == 1;
                color = tag.getColor();
            }
        }
        if (offline) {
            viewHolder.name.setTextColor(StyledAttributes.getColor(activity, R.attr.text_Color_Main));
            viewHolder.name.setAlpha(INACTIVE_ALPHA);
            viewHolder.jid.setAlpha(INACTIVE_ALPHA);
            viewHolder.avatar.setAlpha(INACTIVE_ALPHA);
            viewHolder.tags.setAlpha(INACTIVE_ALPHA);
        } else {
            if (ShowPresenceColoredNames()) {
                viewHolder.name.setTextColor(color != 0 ? color : StyledAttributes.getColor(activity, R.attr.text_Color_Main));
            } else {
                viewHolder.name.setTextColor(StyledAttributes.getColor(activity, R.attr.text_Color_Main));
            }
            viewHolder.name.setAlpha(ACTIVE_ALPHA);
            viewHolder.jid.setAlpha(ACTIVE_ALPHA);
            viewHolder.avatar.setAlpha(ACTIVE_ALPHA);
            viewHolder.tags.setAlpha(ACTIVE_ALPHA);
        }
        AvatarWorkerTask.loadAvatar(item, viewHolder.avatar, R.dimen.avatar);
        return view;
    }

    public void setOnTagClickedListener(OnTagClickedListener listener) {
        this.mOnTagClickedListener = listener;
    }

    public interface OnTagClickedListener {
        void onTagClicked(String tag);
    }

    private static class ViewHolder {
        private TextView name;
        private TextView jid;
        private TextView account;
        private ImageView avatar;
        private FlowLayout tags;

        private ViewHolder() {
        }

        public static ViewHolder get(ContactBinding binding) {
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.name = binding.contactDisplayName;
            viewHolder.jid = binding.contactJid;
            viewHolder.account = binding.account;
            viewHolder.avatar = binding.contactPhoto;
            viewHolder.tags = binding.tags;
            binding.getRoot().setTag(viewHolder);
            return viewHolder;
        }
    }

    private boolean ShowPresenceColoredNames() {
        return getPreferences().getBoolean("presence_colored_names", activity.getResources().getBoolean(R.bool.presence_colored_names));
    }

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
    }
}
