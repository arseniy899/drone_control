package ml.arseniy899.drone_control;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsActivity extends AppCompatActivity
{
	private EditText ipAddrEdit;
	private View connectCheck;
	MemoryWork memoryWork;
	SocketClient client;
	boolean isChecking = false;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		findViewById(R.id.exit).setOnClickListener(view -> finish());
		memoryWork = new MemoryWork(this);
		ipAddrEdit = findViewById(R.id.ipAddrEdit);
		connectCheck = findViewById(R.id.connectCheck);
		String oldIp = memoryWork.loadString("connect-ip");
		if(oldIp.isEmpty())
			oldIp = "r-ho.ml:8765";
		ipAddrEdit.setText(oldIp);
		ipAddrEdit.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
			
			}
			
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
			
			}
			
			@Override
			public void afterTextChanged(Editable editable)
			{
				memoryWork.writeStr("connect-ip",ipAddrEdit.getText()+"");
			}
		});
		connectCheck.setOnClickListener(view -> {
			String addr = ipAddrEdit.getText().toString();
			if(addr.isEmpty())
				ipAddrEdit.setError("Поле не может быть пусто");
			else if(!addr.contains(":"))
				ipAddrEdit.setError("Не указан порт");
			else
			{
				ipAddrEdit.setError(null);
				if(client != null)
					client.disconnect();
				isChecking = true;
				client = new SocketClient(this,addr, new SocketClient.ClientCallback()
				{
					
					@Override
					void onConnectionStateCh(boolean isConnected)
					{
						super.onConnectionStateCh(isConnected);
						runOnUiThread(()->{
							if (isChecking)
							{
								
								if(isConnected)
								{
									Toast.makeText(getBaseContext(),"Успешно",Toast.LENGTH_LONG).show();
									
								}
								else
								{
									Toast.makeText(getBaseContext(),"Ошибка соединения. Проверьте адрес",Toast.LENGTH_LONG).show();
									
								}
								
								
								isChecking = false;
								client.disconnect();
							}
						});
					}
				});
			}
		});
		((TextView)findViewById(R.id.homePathTextView)).setText(LogKeeper.getHomePath());
		findViewById(R.id.homePathBtn).setOnClickListener(v ->
		{
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			Uri uri = Uri.parse(LogKeeper.getHomePath());
			intent.setDataAndType(uri, "text/csv");
			startActivity(Intent.createChooser(intent, "Open folder"));
			
//			Intent intent = new Intent(Intent.ACTION_VIEW);
//			intent.setDataAndType(Uri.parse("file://"+LogKeeper.getHomePath(getBaseContext())), "*/*");
//			intent.addFlags(
//					Intent.FLAG_GRANT_READ_URI_PERMISSION |
//					Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
//					Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
//					Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
//			);
//			intent.addCategory(Intent.CATEGORY_OPENABLE);
//			startActivityForResult(intent, 152);
			
			
		});
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
		client = null;
	}
}
