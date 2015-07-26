package com.mk.interruptionhelper.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by MK on 2015/7/26.
 * When this layout is in the Horizontal orientation and one and only one child is a TextView with a
 * non-null android:ellipsize, this layout will reduce android:maxWidth of that TextView to ensure
 * the siblings are not truncated. This class is useful when that ellipsize-text-view "starts"
 * before other children of this view group. This layout has no effect if:
 * <ul>
 *     <li>the orientation is not horizontal</li>
 *     <li>any child has weights.</li>
 *     <li>more than one child has a non-null android:ellipsize.</li>
 * </ul>
 *
 * <p>The purpose of this horizontal-linear-layout is to ensure that when the sum of widths of the
 * children are greater than this parent, the maximum width of the ellipsize-text-view, is reduced
 * so that no siblings are truncated.</p>
 *
 * <p>For example: Given Text1 has android:ellipsize="end" and Text2 has android:ellipsize="none",
 * as Text1 and/or Text2 grow in width, both will consume more width until Text2 hits the end
 * margin, then Text1 will cease to grow and instead shrink to accommodate any further growth in
 * Text2.</p>
 * <ul>
 * <li>|[text1]|[text2]              |</li>
 * <li>|[text1 text1]|[text2 text2]  |</li>
 * <li>|[text...]|[text2 text2 text2]|</li>
 * </ul>
 */
public class EllipsizeLayout {
}
