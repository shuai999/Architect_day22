package cn.novate.architect_day22;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Email: 2185134304@qq.com
 * Created by Novate 2018/6/16 8:42
 * Version 1.0
 * Params:
 * Description:
*/
public class MainActivity extends AppCompatActivity {

    private TextView mTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 注册，思考为什么要注册？
        EventBus.getDefault().register(this);

        // 进入测试界面
        mTv = (TextView) findViewById(R.id.test_tv);
        mTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,TestActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * threadMode 执行的线程方式
     * priority 执行的优先级
     * sticky 粘性事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN,priority = 50,sticky = true)
    public void test1(String msg){
        // 如果有一个地方用 EventBus 发送一个 String 对象，那么这个方法就会被执行
        Log.e("TAG","msg1 = "+msg);
        mTv.setText(msg);
    }

    /**
     * threadMode 执行的线程方式
     * priority 执行的优先级，值越大优先级越高
     * sticky 粘性事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN,priority = 100,sticky = true)
    public void test2(String msg){
        // 如果有一个地方用 EventBus 发送一个 String 对象，那么这个方法就会被执行
        Log.e("TAG","msg2 = "+msg);
        mTv.setText(msg);
    }

    @Override
    protected void onDestroy() {
        // 解绑，思考为什么要解绑？
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
