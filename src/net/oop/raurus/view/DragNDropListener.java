/**
TUBES 2 OOP
 **/
package net.oop.raurus.view;

import android.view.View;
import android.widget.ListView;


public interface DragNDropListener {

    void onStartDrag(View itemView);


    void onDrag(int x, int y, ListView listView);


    void onStopDrag(View itemView);


    void onDrop(int flatPosFrom, int flatPosTo);
}
