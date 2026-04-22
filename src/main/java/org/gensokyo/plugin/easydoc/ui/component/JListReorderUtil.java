package org.gensokyo.plugin.easydoc.ui.component;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 同一 {@link JList} 内通过拖放调整 {@link DefaultListModel} 行顺序，支持多选为连续/非连续块成组移动。
 */
public final class JListReorderUtil {
    private static final String PREFIX = "org.gensokyo.easydoc.jlistreorder:";

    private JListReorderUtil() {
    }

    public static <T> void install(JList<T> list, DefaultListModel<T> model) {
        list.setDragEnabled(true);
        list.setDropMode(javax.swing.DropMode.INSERT);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                int[] from = list.getSelectedIndices();
                if (from.length == 0) {
                    return null;
                }
                Arrays.sort(from);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < from.length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(from[i]);
                }
                return new IndexTransferable(sb.toString());
            }

            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDrop() || !list.isEnabled()) {
                    return false;
                }
                if (support.getComponent() != list) {
                    return false;
                }
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                if (!(support.getDropLocation() instanceof JList.DropLocation)) {
                    return false;
                }
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                if (!dl.isInsert()) {
                    return false;
                }
                int[] from;
                try {
                    from = readFromIndices(support);
                } catch (Exception e) {
                    return false;
                }
                if (from.length == 0) {
                    return false;
                }
                for (int idx : from) {
                    if (idx < 0 || idx >= model.getSize()) {
                        return false;
                    }
                }
                int to = dl.getIndex();
                if (to < 0) {
                    to = 0;
                } else if (to > model.getSize()) {
                    to = model.getSize();
                }
                // 放置插入位置在「移除前」的坐标系中，按选中下标中略小于 to 的个数向左收缩
                int newTo = to;
                for (int idx : from) {
                    if (idx < to) {
                        newTo--;
                    }
                }
                List<T> items = new ArrayList<>(from.length);
                for (int idx : from) {
                    items.add(model.getElementAt(idx));
                }
                for (int i = from.length - 1; i >= 0; i--) {
                    model.remove(from[i]);
                }
                if (newTo < 0) {
                    newTo = 0;
                } else if (newTo > model.getSize()) {
                    newTo = model.getSize();
                }
                for (int i = 0; i < items.size(); i++) {
                    model.insertElementAt(items.get(i), newTo + i);
                }
                int last = newTo + items.size() - 1;
                list.setSelectionInterval(newTo, last);
                return true;
            }
        });
    }

    private static int[] readFromIndices(TransferSupport support)
            throws UnsupportedFlavorException, IOException {
        String s = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
        if (s == null || !s.startsWith(PREFIX)) {
            throw new UnsupportedFlavorException(DataFlavor.stringFlavor);
        }
        String[] parts = s.substring(PREFIX.length()).split(",");
        if (parts.length == 0) {
            return new int[0];
        }
        int[] r = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            r[i] = Integer.parseInt(parts[i].trim());
        }
        return r;
    }

    private static final class IndexTransferable implements Transferable {
        private static final DataFlavor[] FLAVORS = {DataFlavor.stringFlavor};
        private final String payload;

        private IndexTransferable(String indicesCsv) {
            this.payload = PREFIX + indicesCsv;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return FLAVORS.clone();
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.stringFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (isDataFlavorSupported(flavor)) {
                return payload;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
