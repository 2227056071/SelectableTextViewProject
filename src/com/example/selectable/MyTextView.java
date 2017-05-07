package com.example.selectable;

import org.w3c.dom.Element;

import com.example.viewtest.R;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ClipData;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

public class MyTextView extends TextView {

	private Context mContext;

	protected Element mElement = null;
	private int mTouchX;
	private int mTouchY;

	private Spannable mSpannable;
	private SelectionInfo mSelectionInfo = new SelectionInfo();
	private final static int DEFAULT_SELECTION_LENGTH = 1;
	private BackgroundColorSpan mSpan;
	private int mSelectedColor = 0xFFAFE1F4;
	private int mCursorHandleColor = 0xFF1379D6;
	private CursorHandle mStartHandle;
	private CursorHandle mEndHandle;
	private boolean isHide = true;
	private OperateWindow mOperateWindow;

	public MyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		init();

	}

	public MyTextView(Context context) {
		super(context, null);
	}

	private void init() {
		setText(getText(), TextView.BufferType.SPANNABLE);
		setTextSize(18);
		setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				showSelectView(mTouchX, mTouchY);
				return true;
			}
		});
		setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				mTouchX = (int) event.getX();
				mTouchY = (int) event.getY();
				return false;
			}
		});

		setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				resetSelectionInfo();
				hideSelectView();
			}
		});

		mOperateWindow = new OperateWindow(mContext);
	}

	private void showSelectView(int x, int y) {
		hideSelectView();
		resetSelectionInfo();
		isHide = false;
		if (mStartHandle == null)
			mStartHandle = new CursorHandle(true);
		if (mEndHandle == null)
			mEndHandle = new CursorHandle(false);

		int startOffset = TextLayoutUtil.getPreciseOffset(this, x, y);
		int endOffset = startOffset + DEFAULT_SELECTION_LENGTH;
		if (getText() instanceof Spannable) {
			mSpannable = (Spannable) getText();
		}
		if (mSpannable == null || startOffset >= getText().length()) {
			return;
		}
		selectText(startOffset, endOffset);
		showCursorHandle(mStartHandle);
		showCursorHandle(mEndHandle);
		mOperateWindow.show();
	}

	private void hideSelectView() {
		isHide = true;
		if (mStartHandle != null) {
			mStartHandle.dismiss();
		}
		if (mEndHandle != null) {
			mEndHandle.dismiss();
		}
		if (mOperateWindow != null) {
			mOperateWindow.dismiss();
		}
	}

	private void showCursorHandle(CursorHandle cursorHandle) {
		Layout layout = getLayout();
		int offset = cursorHandle.isLeft ? mSelectionInfo.mStart
				: mSelectionInfo.mEnd;
		cursorHandle.show((int) layout.getPrimaryHorizontal(offset),
				layout.getLineBottom(layout.getLineForOffset(offset)));
	}

	private void resetSelectionInfo() {
		mSelectionInfo.mSelectionContent = null;
		if (mSpannable != null && mSpan != null) {
			mSpannable.removeSpan(mSpan);
			mSpan = null;
		}
	}

	/*
	 * startPos:起始索引 endPos：尾部索引
	 */
	private void selectText(int startPos, int endPos) {
		if (startPos != -1) {
			mSelectionInfo.mStart = startPos;
		}
		if (endPos != -1) {
			mSelectionInfo.mEnd = endPos;
		}
		if (mSelectionInfo.mStart > mSelectionInfo.mEnd) {
			int temp = mSelectionInfo.mStart;
			mSelectionInfo.mStart = mSelectionInfo.mEnd;
			mSelectionInfo.mEnd = temp;
		}

		if (mSpannable != null) {
			if (mSpan == null) {
				mSpan = new BackgroundColorSpan(mSelectedColor);
			}
			mSelectionInfo.mSelectionContent = mSpannable.subSequence(
					mSelectionInfo.mStart, mSelectionInfo.mEnd).toString();
			
			// 调用系统方法设置选中文本的状态
			mSpannable.setSpan(mSpan, mSelectionInfo.mStart,
					mSelectionInfo.mEnd, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	
	/*
	 * 游标类
	 */
	class CursorHandle extends View {

		private final int mCursorHandleSize = 48;
		private PopupWindow mPopupWindow;
		private Paint mPaint;

		private int mCircleRadius = mCursorHandleSize / 2;
		private int mWidth = mCircleRadius * 2;
		private int mHeight = mCircleRadius * 2;
		private int mPadding = 25;
		private boolean isLeft;

		public CursorHandle(boolean isLeft) {
			super(mContext);
			this.isLeft = isLeft;
			mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			mPaint.setColor(mCursorHandleColor);

			mPopupWindow = new PopupWindow(this);
			mPopupWindow.setClippingEnabled(false);
			mPopupWindow.setWidth(mWidth + mPadding * 2);
			mPopupWindow.setHeight(mHeight + mPadding / 2);
			invalidate();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawCircle(mCircleRadius + mPadding, mCircleRadius,
					mCircleRadius, mPaint);
			if (isLeft) {
				canvas.drawRect(mCircleRadius + mPadding, 0, mCircleRadius * 2
						+ mPadding, mCircleRadius, mPaint);
			} else {
				canvas.drawRect(mPadding, 0, mCircleRadius + mPadding,
						mCircleRadius, mPaint);
			}
		}

		private int mAdjustX;
		private int mAdjustY;

		private int mBeforeDragStart;
		private int mBeforeDragEnd;

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mBeforeDragStart = mSelectionInfo.mStart;
				mBeforeDragEnd = mSelectionInfo.mEnd;
				mAdjustX = (int) event.getX();
				mAdjustY = (int) event.getY();
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mOperateWindow.show();
				break;
			case MotionEvent.ACTION_MOVE:
				mOperateWindow.dismiss();
				int rawX = (int) event.getRawX();
				int rawY = (int) event.getRawY();
				update(rawX + mAdjustX - mWidth, rawY + mAdjustY - mHeight);
				break;
			}
			return true;
		}

		private void changeDirection() {
			isLeft = !isLeft;
			invalidate();
		}

		public void dismiss() {
			mPopupWindow.dismiss();
		}

		private int[] mTempCoors = new int[2];

		public void update(int x, int y) {
			MyTextView.this.getLocationInWindow(mTempCoors);
			int oldOffset;
			if (isLeft) {
				oldOffset = mSelectionInfo.mStart;
			} else {
				oldOffset = mSelectionInfo.mEnd;
			}

			y -= mTempCoors[1];

			int offset = TextLayoutUtil.getHysteresisOffset(MyTextView.this, x,
					y, oldOffset);

			if (offset != oldOffset) {
				resetSelectionInfo();
				if (isLeft) {
					if (offset > mBeforeDragEnd) {
						CursorHandle handle = getCursorHandle(false);
						changeDirection();
						handle.changeDirection();
						mBeforeDragStart = mBeforeDragEnd;
						selectText(mBeforeDragEnd, offset);
						handle.updateCursorHandle();
					} else {
						selectText(offset, -1);
					}
					updateCursorHandle();
				} else {
					if (offset < mBeforeDragStart) {
						CursorHandle handle = getCursorHandle(true);
						handle.changeDirection();
						changeDirection();
						mBeforeDragEnd = mBeforeDragStart;
						selectText(offset, mBeforeDragStart);
						handle.updateCursorHandle();
					} else {
						selectText(mBeforeDragStart, offset);
					}
					updateCursorHandle();
				}
			}
		}

		private void updateCursorHandle() {
			MyTextView.this.getLocationInWindow(mTempCoors);
			Layout layout = MyTextView.this.getLayout();
			if (isLeft) {
				mPopupWindow.update(
						(int) layout
								.getPrimaryHorizontal(mSelectionInfo.mStart)
								- mWidth + getExtraX(),
						layout.getLineBottom(layout
								.getLineForOffset(mSelectionInfo.mStart))
								+ getExtraY(), -1, -1);
			} else {
				mPopupWindow.update(
						(int) layout.getPrimaryHorizontal(mSelectionInfo.mEnd)
								+ getExtraX(),
						layout.getLineBottom(layout
								.getLineForOffset(mSelectionInfo.mEnd))
								+ getExtraY(), -1, -1);
			}
		}

		public void show(int x, int y) {
			MyTextView.this.getLocationInWindow(mTempCoors);
			int offset = isLeft ? mWidth : 0;
			mPopupWindow.showAtLocation(MyTextView.this, Gravity.NO_GRAVITY, x
					- offset + getExtraX(), y + getExtraY());
		}

		public int getExtraX() {
			return mTempCoors[0] - mPadding + MyTextView.this.getPaddingLeft();
		}

		public int getExtraY() {
			return mTempCoors[1] + MyTextView.this.getPaddingTop();
		}
	}

	private CursorHandle getCursorHandle(boolean isLeft) {
		if (mStartHandle.isLeft == isLeft) {
			return mStartHandle;
		} else {
			return mEndHandle;
		}
	}

	/*
	 * 操作框
	 */
	private class OperateWindow {

		private PopupWindow mWindow;
		private int[] mTempCoors = new int[2];

		private int mWidth;
		private int mHeight;

		public OperateWindow(final Context context) {
			View contentView = LayoutInflater.from(context).inflate(
					R.layout.layout_operate_windows, null);
			contentView.measure(View.MeasureSpec.makeMeasureSpec(0,
					View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
					.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
			mWidth = contentView.getMeasuredWidth();
			mHeight = contentView.getMeasuredHeight();
			mWindow = new PopupWindow(contentView,
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, false);
			mWindow.setClippingEnabled(false);

			contentView.findViewById(R.id.tv_copy).setOnClickListener(
					new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							ClipboardManager clip = (ClipboardManager) mContext
									.getSystemService(Context.CLIPBOARD_SERVICE);
							clip.setPrimaryClip(ClipData.newPlainText(
									mSelectionInfo.mSelectionContent,
									mSelectionInfo.mSelectionContent));
							resetSelectionInfo();
							hideSelectView();
						}
					});
			contentView.findViewById(R.id.tv_select_all).setOnClickListener(
					new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							hideSelectView();
							selectText(0, getText().length());
							isHide = false;
							showCursorHandle(mStartHandle);
							showCursorHandle(mEndHandle);
							mOperateWindow.show();
						}
					});
		}

		public void show() {
			getLocationInWindow(mTempCoors);
			Layout layout = getLayout();
			int posX = (int) layout.getPrimaryHorizontal(mSelectionInfo.mStart)
					+ mTempCoors[0];
			int posY = layout.getLineTop(layout
					.getLineForOffset(mSelectionInfo.mStart))
					+ mTempCoors[1]
					- mHeight - 16;
			if (posX <= 0)
				posX = 16;
			if (posY < 0)
				posY = 16;
			if (posX + mWidth > TextLayoutUtil.getScreenWidth(mContext)) {
				posX = TextLayoutUtil.getScreenWidth(mContext) - mWidth - 16;
			}
			mWindow.showAtLocation(MyTextView.this, Gravity.NO_GRAVITY, posX,
					posY);
		}

		public void dismiss() {
			mWindow.dismiss();
		}

		public boolean isShowing() {
			return mWindow.isShowing();
		}
	}

}
