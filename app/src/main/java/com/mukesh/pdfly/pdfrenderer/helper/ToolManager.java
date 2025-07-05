package com.mukesh.pdfly.pdfrenderer.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mukesh.pdfly.R;

public class ToolManager {

    public enum ToolType {
        DRAW, UNDO, REDO, BLOCK_ACTION, SHAPE, SIGNATURE, COMMENT, TEXT, DATE
    }

    public interface ToolActionHandler {
        void onToolSelected(ToolType type, int index);
    }

    private final Context context;
    private final LinearLayout toolContainer;
    private final ToolActionHandler handler;
    private int selectedToolIndex = -1;
    private ImageView drawToolIconView;

    private final int[] toolIcons = {
            R.drawable.ic_pencil, R.drawable.ic_undo, R.drawable.ic_redo,
            R.drawable.ic_select_24dp, R.drawable.ic_shapes_24dp,
            R.drawable.ic_signature_solid, R.drawable.ic_checkmark_transparent, R.drawable.ic_text_symbol_24dp, R.drawable.ic_calendar_add_light
    };

    private final ToolType[] tools = {
            ToolType.DRAW, ToolType.UNDO, ToolType.REDO,
            ToolType.BLOCK_ACTION, ToolType.SHAPE, ToolType.SIGNATURE, ToolType.COMMENT, ToolType.TEXT, ToolType.DATE
    };


    public ToolManager(Context context, LinearLayout toolContainer, ToolActionHandler handler) {
        this.context = context;
        this.toolContainer = toolContainer;
        this.handler = handler;
        setupToolbar();
    }

    private void setupToolbar() {
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < tools.length; i++) {
            View toolItem = inflater.inflate(R.layout.item_tool_button, toolContainer, false);
            ImageView btnTool = toolItem.findViewById(R.id.btnTool);
            btnTool.setImageResource(toolIcons[i]);

            if (tools[i] == ToolType.DRAW) {
                drawToolIconView = btnTool;
            }

            int index = i;
            toolItem.setOnClickListener(v -> onToolClicked(index));
            toolContainer.addView(toolItem);
        }
    }

    private void onToolClicked(int index) {
        View tapped = toolContainer.getChildAt(index);

        if (selectedToolIndex == index && tools[index] == ToolType.DRAW) {
            handler.onToolSelected(ToolType.DRAW, index); // Repeat click for pen popup
            return;
        }

        if (selectedToolIndex != -1 && !(tools[index] == ToolType.UNDO || tools[index] == ToolType.REDO)) {
            toolContainer.getChildAt(selectedToolIndex).setSelected(false);
        }

        if (!(tools[index] == ToolType.UNDO || tools[index] == ToolType.REDO)) {
            tapped.setSelected(true);
            selectedToolIndex = index;
        }

        handler.onToolSelected(tools[index], index);
    }

    public ImageView getDrawToolIconView() {
        return drawToolIconView;
    }
}

