package io.digibyte.tools.list;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class ListItemViewHolder extends RecyclerView.ViewHolder
{
    private View view;
    protected ListItemData theItemData;
    public ListItemData getItemData() { return theItemData; }

    public ListItemViewHolder(View itemView)
    {
        super(itemView);
        view = itemView;
    }

    public View getView() {
        return view;
    }

    public void process(ListItemData aListItemData)
    {
        theItemData = aListItemData;
    }
}
