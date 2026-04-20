package com.unity3d.player;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

/* JADX INFO: renamed from: com.unity3d.player.i */
/* JADX INFO: loaded from: classes2.dex */
public final class DialogC0918i extends Dialog implements TextWatcher, View.OnClickListener {

    /* JADX INFO: renamed from: d */
    private static int f372d = 1627389952;

    /* JADX INFO: renamed from: e */
    private static int f373e = -1;

    /* JADX INFO: renamed from: a */
    public boolean f374a;

    /* JADX INFO: renamed from: b */
    private Context f375b;

    /* JADX INFO: renamed from: c */
    private UnityPlayer f376c;

    /* JADX INFO: renamed from: f */
    private int f377f;

    /* JADX INFO: renamed from: g */
    private boolean f378g;

    /* JADX INFO: renamed from: com.unity3d.player.i$a */
    private static final class a {

        /* JADX INFO: renamed from: a */
        private static final int f384a = View.generateViewId();

        /* JADX INFO: renamed from: b */
        private static final int f385b = View.generateViewId();

        /* JADX INFO: renamed from: c */
        private static final int f386c = View.generateViewId();
    }

    public DialogC0918i(Context context, UnityPlayer unityPlayer, String str, int i, boolean z, boolean z2, boolean z3, String str2, int i2, boolean z4, boolean z5) {
        super(context);
        this.f375b = null;
        this.f376c = null;
        this.f375b = context;
        this.f376c = unityPlayer;
        Window window = getWindow();
        this.f374a = z5;
        window.requestFeature(1);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.gravity = 80;
        attributes.x = 0;
        attributes.y = 0;
        window.setAttributes(attributes);
        window.setBackgroundDrawable(new ColorDrawable(0));
        final View viewCreateSoftInputView = createSoftInputView();
        setContentView(viewCreateSoftInputView);
        window.setLayout(-1, -2);
        window.clearFlags(2);
        window.clearFlags(134217728);
        window.clearFlags(67108864);
        if (!this.f374a) {
            window.addFlags(32);
            window.addFlags(262144);
        }
        EditText editText = (EditText) findViewById(a.f385b);
        Button button = (Button) findViewById(a.f384a);
        m375a(editText, str, i, z, z2, z3, str2, i2);
        button.setOnClickListener(this);
        this.f377f = editText.getCurrentTextColor();
        m385a(z4);
        this.f376c.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.unity3d.player.i.1
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public final void onGlobalLayout() {
                if (viewCreateSoftInputView.isShown()) {
                    Rect rect = new Rect();
                    DialogC0918i.this.f376c.getWindowVisibleDisplayFrame(rect);
                    int[] iArr = new int[2];
                    DialogC0918i.this.f376c.getLocationOnScreen(iArr);
                    Point point = new Point(rect.left - iArr[0], rect.height() - viewCreateSoftInputView.getHeight());
                    Point point2 = new Point();
                    DialogC0918i.this.getWindow().getWindowManager().getDefaultDisplay().getSize(point2);
                    int height = DialogC0918i.this.f376c.getHeight() - point2.y;
                    int height2 = DialogC0918i.this.f376c.getHeight() - point.y;
                    if (height2 != height + viewCreateSoftInputView.getHeight()) {
                        DialogC0918i.this.f376c.reportSoftInputIsVisible(true);
                    } else {
                        DialogC0918i.this.f376c.reportSoftInputIsVisible(false);
                    }
                    DialogC0918i.this.f376c.reportSoftInputArea(new Rect(point.x, point.y, viewCreateSoftInputView.getWidth(), height2));
                }
            }
        });
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: com.unity3d.player.i.2
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view, boolean z6) {
                if (z6) {
                    DialogC0918i.this.getWindow().setSoftInputMode(5);
                }
            }
        });
        editText.requestFocus();
    }

    /* JADX INFO: renamed from: a */
    private static int m373a(int i, boolean z, boolean z2, boolean z3) {
        int i2 = (z ? 32768 : 524288) | (z2 ? 131072 : 0) | (z3 ? 128 : 0);
        if (i < 0 || i > 11) {
            return i2;
        }
        int[] iArr = {1, 16385, 12290, 17, 2, 3, 8289, 33, 1, 16417, 17, 8194};
        return (iArr[i] & 2) != 0 ? iArr[i] : iArr[i] | i2;
    }

    /* JADX INFO: renamed from: a */
    private void m375a(EditText editText, String str, int i, boolean z, boolean z2, boolean z3, String str2, int i2) {
        editText.setImeOptions(6);
        editText.setText(str);
        editText.setHint(str2);
        editText.setHintTextColor(f372d);
        editText.setInputType(m373a(i, z, z2, z3));
        editText.setImeOptions(33554432);
        if (i2 > 0) {
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(i2)});
        }
        editText.addTextChangedListener(this);
        editText.setSelection(editText.getText().length());
        editText.setClickable(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: a */
    public void m377a(String str, boolean z) {
        ((EditText) findViewById(a.f385b)).setSelection(0, 0);
        this.f376c.reportSoftInputStr(str, 1, z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX INFO: renamed from: b */
    public String m378b() {
        EditText editText = (EditText) findViewById(a.f385b);
        if (editText == null) {
            return null;
        }
        return editText.getText().toString();
    }

    /* JADX INFO: renamed from: a */
    public final String m381a() {
        InputMethodSubtype currentInputMethodSubtype = ((InputMethodManager) this.f375b.getSystemService("input_method")).getCurrentInputMethodSubtype();
        if (currentInputMethodSubtype == null) {
            return null;
        }
        String locale = currentInputMethodSubtype.getLocale();
        if (locale != null && !locale.equals("")) {
            return locale;
        }
        return currentInputMethodSubtype.getMode() + " " + currentInputMethodSubtype.getExtraValue();
    }

    /* JADX INFO: renamed from: a */
    public final void m382a(int i) {
        EditText editText = (EditText) findViewById(a.f385b);
        if (editText != null) {
            if (i > 0) {
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(i)});
            } else {
                editText.setFilters(new InputFilter[0]);
            }
        }
    }

    /* JADX INFO: renamed from: a */
    public final void m383a(int i, int i2) {
        int i3;
        EditText editText = (EditText) findViewById(a.f385b);
        if (editText == null || editText.getText().length() < (i3 = i2 + i)) {
            return;
        }
        editText.setSelection(i, i3);
    }

    /* JADX INFO: renamed from: a */
    public final void m384a(String str) {
        EditText editText = (EditText) findViewById(a.f385b);
        if (editText != null) {
            editText.setText(str);
            editText.setSelection(str.length());
        }
    }

    /* JADX INFO: renamed from: a */
    public final void m385a(boolean z) {
        this.f378g = z;
        EditText editText = (EditText) findViewById(a.f385b);
        Button button = (Button) findViewById(a.f384a);
        View viewFindViewById = findViewById(a.f386c);
        if (z) {
            editText.setBackgroundColor(0);
            editText.setTextColor(0);
            editText.setCursorVisible(false);
            editText.setHighlightColor(0);
            editText.setOnClickListener(this);
            editText.setLongClickable(false);
            button.setTextColor(0);
            viewFindViewById.setBackgroundColor(0);
            viewFindViewById.setOnClickListener(this);
            return;
        }
        editText.setBackgroundColor(f373e);
        editText.setTextColor(this.f377f);
        editText.setCursorVisible(true);
        editText.setOnClickListener(null);
        editText.setLongClickable(true);
        button.setClickable(true);
        button.setTextColor(this.f377f);
        viewFindViewById.setBackgroundColor(f373e);
        viewFindViewById.setOnClickListener(null);
    }

    @Override // android.text.TextWatcher
    public final void afterTextChanged(Editable editable) {
        this.f376c.reportSoftInputStr(editable.toString(), 0, false);
        EditText editText = (EditText) findViewById(a.f385b);
        int selectionStart = editText.getSelectionStart();
        this.f376c.reportSoftInputSelection(selectionStart, editText.getSelectionEnd() - selectionStart);
    }

    @Override // android.text.TextWatcher
    public final void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    protected final View createSoftInputView() {
        RelativeLayout relativeLayout = new RelativeLayout(this.f375b);
        relativeLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        relativeLayout.setBackgroundColor(f373e);
        relativeLayout.setId(a.f386c);
        EditText editText = new EditText(this.f375b) { // from class: com.unity3d.player.i.3
            @Override // android.widget.TextView, android.view.View
            public final boolean onKeyPreIme(int i, KeyEvent keyEvent) {
                if (i == 4) {
                    DialogC0918i dialogC0918i = DialogC0918i.this;
                    dialogC0918i.m377a(dialogC0918i.m378b(), true);
                    return true;
                }
                if (i == 84) {
                    return true;
                }
                return super.onKeyPreIme(i, keyEvent);
            }

            @Override // android.widget.TextView
            protected final void onSelectionChanged(int i, int i2) {
                DialogC0918i.this.f376c.reportSoftInputSelection(i, i2 - i);
            }

            @Override // android.widget.TextView, android.view.View
            public final void onWindowFocusChanged(boolean z) {
                super.onWindowFocusChanged(z);
                if (z) {
                    ((InputMethodManager) DialogC0918i.this.f375b.getSystemService("input_method")).showSoftInput(this, 0);
                }
            }
        };
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-1, -2);
        layoutParams.addRule(15);
        layoutParams.addRule(0, a.f384a);
        editText.setLayoutParams(layoutParams);
        editText.setId(a.f385b);
        relativeLayout.addView(editText);
        Button button = new Button(this.f375b);
        button.setText(this.f375b.getResources().getIdentifier("ok", "string", "android"));
        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(-2, -2);
        layoutParams2.addRule(15);
        layoutParams2.addRule(11);
        button.setLayoutParams(layoutParams2);
        button.setId(a.f384a);
        button.setBackgroundColor(0);
        relativeLayout.addView(button);
        ((EditText) relativeLayout.findViewById(a.f385b)).setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.unity3d.player.i.4
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == 6) {
                    DialogC0918i dialogC0918i = DialogC0918i.this;
                    dialogC0918i.m377a(dialogC0918i.m378b(), false);
                }
                return false;
            }
        });
        relativeLayout.setPadding(16, 16, 16, 16);
        return relativeLayout;
    }

    @Override // android.app.Dialog, android.view.Window.Callback
    public final boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (this.f374a || !(motionEvent.getAction() == 4 || this.f378g)) {
            return super.dispatchTouchEvent(motionEvent);
        }
        return true;
    }

    @Override // android.app.Dialog
    public final void onBackPressed() {
        m377a(m378b(), true);
    }

    @Override // android.view.View.OnClickListener
    public final void onClick(View view) {
        m377a(m378b(), false);
    }

    @Override // android.text.TextWatcher
    public final void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }
}
