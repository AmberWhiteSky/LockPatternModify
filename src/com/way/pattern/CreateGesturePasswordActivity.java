package com.way.pattern;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.way.view.LockPatternUtils;
import com.way.view.LockPatternView;
import com.way.view.LockPatternView.Cell;
import com.way.view.LockPatternView.DisplayMode;

/**
 * 创建手势密码类
 * 
 * @author jgduan 手势密码创建
 * 
 */
public class CreateGesturePasswordActivity extends Activity implements
		OnClickListener {
	private static final int ID_EMPTY_MESSAGE = -1;
	private static final String KEY_UI_STAGE = "uiStage";
	private static final String KEY_PATTERN_CHOICE = "chosenPattern";
	/** 中间圆点解锁图案 **/
	private LockPatternView mLockPatternView;
	/** 底部右侧按钮 **/
	private Button mFooterRightButton;
	/** 底部左侧按钮 **/
	private Button mFooterLeftButton;
	/** 顶部文本 **/
	protected TextView mHeaderText;
	/** 用来演示如何绘制解锁图案 **/
	private final List<LockPatternView.Cell> mAnimatePattern = new ArrayList<LockPatternView.Cell>();
	/** 顶部视图,标记已设置的密码图形 **/
	private View mPreviewViews[][] = new View[3][3];
	protected List<LockPatternView.Cell> mChosenPattern = null;
	private Toast mToast;
	private Stage mUiStage = Stage.Introduction;

	/**
	 * 底部左侧按钮
	 */
	enum LeftButtonMode {

		// 在不同状态下为按钮设置不同的文字
		// <Cancel-取消;CancelDisabled-禁用取消;Retry-重试;RetryDisabled-禁用重试;Gone-过去的>
		Cancel(android.R.string.cancel, true), CancelDisabled(
				android.R.string.cancel, false), Retry(
				R.string.lockpattern_retry_button_text, true), RetryDisabled(
				R.string.lockpattern_retry_button_text, false), Gone(
				ID_EMPTY_MESSAGE, false);

		/**
		 * @param text
		 *            指定显示文本的样式
		 * @param enabled
		 *            是否被启用
		 */
		LeftButtonMode(int text, boolean enabled) {
			this.text = text;
			this.enabled = enabled;
		}

		final int text;
		final boolean enabled;
	}

	/**
	 * 底部右侧按钮
	 */
	enum RightButtonMode {

		// 在不同状态下为按钮设置不同的文字
		// <Continue-继续;ContinueDisabled-禁用继续;Confirm-确认;ConfirmDisabled-禁用确认;Ok-搞定>
		Continue(R.string.lockpattern_continue_button_text, true), ContinueDisabled(
				R.string.lockpattern_continue_button_text, false), Confirm(
				R.string.lockpattern_confirm_button_text, true), ConfirmDisabled(
				R.string.lockpattern_confirm_button_text, false), Ok(
				android.R.string.ok, true);

		/**
		 * @param text
		 *            指定显示文本的样式
		 * @param enabled
		 *            是否被启用
		 */
		RightButtonMode(int text, boolean enabled) {
			this.text = text;
			this.enabled = enabled;
		}

		final int text;
		final boolean enabled;
	}

	/**
	 * 用户根据需要选择对应状态 受保护的枚举类(作用范围-当前包类,不含子类)
	 */
	protected enum Stage {

		// 根据状态不同启用按钮
		// <Introduction-初步绘制;HelpScreen-帮助屏幕;ChoiceTooShort-选择太短;FirstChoiceValid-首次选择有效;
		// NeedToConfirm-再次确认;ChoiceConfirmed-选择确认>
		Introduction(R.string.lockpattern_recording_intro_header,
				LeftButtonMode.Cancel, RightButtonMode.ContinueDisabled,
				ID_EMPTY_MESSAGE, true), HelpScreen(
				R.string.lockpattern_settings_help_how_to_record,
				LeftButtonMode.Gone, RightButtonMode.Ok, ID_EMPTY_MESSAGE,
				false), ChoiceTooShort(
				R.string.lockpattern_recording_incorrect_too_short,
				LeftButtonMode.Retry, RightButtonMode.ContinueDisabled,
				ID_EMPTY_MESSAGE, true), FirstChoiceValid(
				R.string.lockpattern_pattern_entered_header,
				LeftButtonMode.Retry, RightButtonMode.Continue,
				ID_EMPTY_MESSAGE, false), NeedToConfirm(
				R.string.lockpattern_need_to_confirm, LeftButtonMode.Cancel,
				RightButtonMode.ConfirmDisabled, ID_EMPTY_MESSAGE, true), ConfirmWrong(
				R.string.lockpattern_need_to_unlock_wrong,
				LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled,
				ID_EMPTY_MESSAGE, true), ChoiceConfirmed(
				R.string.lockpattern_pattern_confirmed_header,
				LeftButtonMode.Cancel, RightButtonMode.Confirm,
				ID_EMPTY_MESSAGE, false);

		/**
		 * @param headerMessage
		 *            显示在顶部
		 * @param leftMode
		 *            左侧按钮样式
		 * @param rightMode
		 *            右侧按钮样式
		 * @param footerMessage
		 *            显示在底部
		 * @param patternEnabled
		 *            是否被启用
		 */
		Stage(int headerMessage, LeftButtonMode leftMode,
				RightButtonMode rightMode, int footerMessage,
				boolean patternEnabled) {
			this.headerMessage = headerMessage;
			this.leftMode = leftMode;
			this.rightMode = rightMode;
			this.footerMessage = footerMessage;
			this.patternEnabled = patternEnabled;
		}

		final int headerMessage;
		final LeftButtonMode leftMode;
		final RightButtonMode rightMode;
		final int footerMessage;
		final boolean patternEnabled;
	}

	/**
	 * 弹出提示信息
	 * 
	 * @param message
	 */
	private void showToast(CharSequence message) {

		if (null == mToast) {
			mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		} else {
			mToast.setText(message);
		}

		mToast.show();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gesturepassword_create);
		// 初始化演示动画--绘制解锁图案
		mAnimatePattern.add(LockPatternView.Cell.of(0, 0));
		mAnimatePattern.add(LockPatternView.Cell.of(0, 1));
		mAnimatePattern.add(LockPatternView.Cell.of(1, 1));
		mAnimatePattern.add(LockPatternView.Cell.of(2, 1));
		mAnimatePattern.add(LockPatternView.Cell.of(2, 2));
		
		// 初始化中间解锁区域
		mLockPatternView = (LockPatternView) this
				.findViewById(R.id.gesturepwd_create_lockview);
		mHeaderText = (TextView) findViewById(R.id.gesturepwd_create_text);
		mLockPatternView.setOnPatternListener(mChooseNewLockPatternListener);
		mLockPatternView.setTactileFeedbackEnabled(true);
		
		// 初始化底部按钮
		mFooterRightButton = (Button) this.findViewById(R.id.right_btn);
		mFooterLeftButton = (Button) this.findViewById(R.id.reset_btn);
		mFooterRightButton.setOnClickListener(this);
		mFooterLeftButton.setOnClickListener(this);
		
		// 初始化顶部图案
		initPreviewViews();
		
		// 判断保存实例的状态是否为空
		if (savedInstanceState == null) {// 如果为空
			// 启用帮助模式
			updateStage(Stage.Introduction);
			updateStage(Stage.HelpScreen);
		} else {// 如果存在实例
			// 读取保存的状态
			final String patternString = savedInstanceState
					.getString(KEY_PATTERN_CHOICE);
			if (patternString != null) {// 如果读取结果不为空
				// 直接进入对应状态-解密
				mChosenPattern = LockPatternUtils
						.stringToPattern(patternString);
			}
			updateStage(Stage.values()[savedInstanceState.getInt(KEY_UI_STAGE)]);
		}

	}

	/**
	 * 初始化顶部视图图案
	 */
	private void initPreviewViews() {
		mPreviewViews = new View[3][3];
		mPreviewViews[0][0] = findViewById(R.id.gesturepwd_setting_preview_0);
		mPreviewViews[0][1] = findViewById(R.id.gesturepwd_setting_preview_1);
		mPreviewViews[0][2] = findViewById(R.id.gesturepwd_setting_preview_2);
		mPreviewViews[1][0] = findViewById(R.id.gesturepwd_setting_preview_3);
		mPreviewViews[1][1] = findViewById(R.id.gesturepwd_setting_preview_4);
		mPreviewViews[1][2] = findViewById(R.id.gesturepwd_setting_preview_5);
		mPreviewViews[2][0] = findViewById(R.id.gesturepwd_setting_preview_6);
		mPreviewViews[2][1] = findViewById(R.id.gesturepwd_setting_preview_7);
		mPreviewViews[2][2] = findViewById(R.id.gesturepwd_setting_preview_8);
	}

	/**
	 * 修改顶部视图图案
	 */
	private void updatePreviewViews() {
		if (mChosenPattern == null)
			return;
		Log.i("way", "result = " + mChosenPattern.toString());
		for (LockPatternView.Cell cell : mChosenPattern) {
			Log.i("way", "cell.getRow() = " + cell.getRow()
					+ ", cell.getColumn() = " + cell.getColumn());
			mPreviewViews[cell.getRow()][cell.getColumn()]
					.setBackgroundResource(R.drawable.gesture_create_grid_selected);

		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_UI_STAGE, mUiStage.ordinal());
		if (mChosenPattern != null) {// 如果mChosenPattern不为空
			// 标记当前状态
			outState.putString(KEY_PATTERN_CHOICE,
					LockPatternUtils.patternToString(mChosenPattern));
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (mUiStage == Stage.HelpScreen) {// 如果在帮助阶段
				updateStage(Stage.Introduction);// 进入到引导阶段
				return true;
			}
		}
		if (keyCode == KeyEvent.KEYCODE_MENU && mUiStage == Stage.Introduction) {
			updateStage(Stage.HelpScreen);// 进入到帮助阶段
			return true;
		}
		return false;
	}

	/**
	 * 通知清除中间区域图案
	 */
	private Runnable mClearPatternRunnable = new Runnable() {
		public void run() {
			mLockPatternView.clearPattern();
		}
	};

	/**
	 * 监听中部区域所处模式,做出相关动作
	 */
	protected LockPatternView.OnPatternListener mChooseNewLockPatternListener = new LockPatternView.OnPatternListener() {

		/**
		 * 开始模式
		 */
		public void onPatternStart() {
			// 删除回调
			mLockPatternView.removeCallbacks(mClearPatternRunnable);
			patternInProgress();// 开始设置密码阶段提示用户
		}

		/**
		 * 清除模式
		 */
		public void onPatternCleared() {
			mLockPatternView.removeCallbacks(mClearPatternRunnable);
		}
		
		/**
		 * 检测模式
		 */
		public void onPatternDetected(List<LockPatternView.Cell> pattern) {
			if (pattern == null)
				return;
			// 根据不同杰作做出反应
			if (mUiStage == Stage.NeedToConfirm
					|| mUiStage == Stage.ConfirmWrong) {
				if (mChosenPattern == null)
					throw new IllegalStateException(
							"null chosen pattern in stage 'need to confirm");
				if (mChosenPattern.equals(pattern)) {
					updateStage(Stage.ChoiceConfirmed);
				} else {
					updateStage(Stage.ConfirmWrong);
				}
			} else if (mUiStage == Stage.Introduction
					|| mUiStage == Stage.ChoiceTooShort) {
				if (pattern.size() < LockPatternUtils.MIN_LOCK_PATTERN_SIZE) {
					updateStage(Stage.ChoiceTooShort);
				} else {
					mChosenPattern = new ArrayList<LockPatternView.Cell>(
							pattern);
					updateStage(Stage.FirstChoiceValid);
				}
			} else {
				throw new IllegalStateException("Unexpected stage " + mUiStage
						+ " when " + "entering the pattern.");
			}
		}

		public void onPatternCellAdded(List<Cell> pattern) {

		}

		/**
		 * 设置密码阶段提示用户<同时禁用底部按钮>
		 */
		private void patternInProgress() {
			mHeaderText.setText(R.string.lockpattern_recording_inprogress);
			mFooterLeftButton.setEnabled(false);
			mFooterRightButton.setEnabled(false);
		}
	};

	/**
	 * 更新阶段
	 * @param stage
	 */
	private void updateStage(Stage stage) {
		mUiStage = stage;
		if (stage == Stage.ChoiceTooShort) {
			mHeaderText.setText(getResources().getString(stage.headerMessage,
					LockPatternUtils.MIN_LOCK_PATTERN_SIZE));
		} else {
			mHeaderText.setText(stage.headerMessage);
		}

		if (stage.leftMode == LeftButtonMode.Gone) {
			mFooterLeftButton.setVisibility(View.GONE);
		} else {
			mFooterLeftButton.setVisibility(View.VISIBLE);
			mFooterLeftButton.setText(stage.leftMode.text);
			mFooterLeftButton.setEnabled(stage.leftMode.enabled);
		}

		mFooterRightButton.setText(stage.rightMode.text);
		mFooterRightButton.setEnabled(stage.rightMode.enabled);

		// same for whether the patten is enabled
		if (stage.patternEnabled) {
			mLockPatternView.enableInput();
		} else {
			mLockPatternView.disableInput();
		}

		mLockPatternView.setDisplayMode(DisplayMode.Correct);

		switch (mUiStage) {
		case Introduction:// 引用阶段
			mLockPatternView.clearPattern();
			break;
		case HelpScreen:// 帮助阶段
			mLockPatternView.setPattern(DisplayMode.Animate, mAnimatePattern);
			break;
		case ChoiceTooShort:// 选择长度过短阶段
			mLockPatternView.setDisplayMode(DisplayMode.Wrong);
			postClearPatternRunnable();
			break;
		case FirstChoiceValid:// 第一次设置阶段
			break;
		case NeedToConfirm:// 第二次设置阶段
			mLockPatternView.clearPattern();
			updatePreviewViews();
			break;
		case ConfirmWrong:// 第二次出错阶段
			mLockPatternView.setDisplayMode(DisplayMode.Wrong);
			postClearPatternRunnable();
			break;
		case ChoiceConfirmed:// 确认阶段
			break;
		}

	}

	/**
	 * 清除错误的花样,除非已经开始绘制新的图样
	 */
	private void postClearPatternRunnable() {
		mLockPatternView.removeCallbacks(mClearPatternRunnable);
		mLockPatternView.postDelayed(mClearPatternRunnable, 2000);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.reset_btn:
			if (mUiStage.leftMode == LeftButtonMode.Retry) {
				mChosenPattern = null;
				mLockPatternView.clearPattern();
				updateStage(Stage.Introduction);
			} else if (mUiStage.leftMode == LeftButtonMode.Cancel) {
				// They are canceling the entire wizard
				finish();
			} else {
				throw new IllegalStateException(
						"left footer button pressed, but stage of " + mUiStage
								+ " doesn't make sense");
			}

			break;
		case R.id.right_btn:
			if (mUiStage.rightMode == RightButtonMode.Continue) {
				if (mUiStage != Stage.FirstChoiceValid) {
					throw new IllegalStateException("expected ui stage "
							+ Stage.FirstChoiceValid + " when button is "
							+ RightButtonMode.Continue);
				}
				updateStage(Stage.NeedToConfirm);
			} else if (mUiStage.rightMode == RightButtonMode.Confirm) {
				if (mUiStage != Stage.ChoiceConfirmed) {
					throw new IllegalStateException("expected ui stage "
							+ Stage.ChoiceConfirmed + " when button is "
							+ RightButtonMode.Confirm);
				}
				saveChosenPatternAndFinish();
			} else if (mUiStage.rightMode == RightButtonMode.Ok) {
				if (mUiStage != Stage.HelpScreen) {
					throw new IllegalStateException(
							"Help screen is only mode with ok button, but "
									+ "stage is " + mUiStage);
				}
				mLockPatternView.clearPattern();
				mLockPatternView.setDisplayMode(DisplayMode.Correct);
				updateStage(Stage.Introduction);
			}
			break;
		}
	}

	/**
	 * 密码设置完成
	 */
	private void saveChosenPatternAndFinish() {
		App.getInstance().getLockPatternUtils().saveLockPattern(mChosenPattern);
		showToast("密码设置成功");
		startActivity(new Intent(this, UnlockGesturePasswordActivity.class));
		finish();
	}
}
