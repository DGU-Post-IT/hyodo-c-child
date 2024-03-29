package com.postit.mymomsweather.presentation.calendar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.postit.mymomsweather.presentation.community.CommunityActivity;
import com.postit.mymomsweather.presentation.game.GameActivity;
import com.postit.mymomsweather.util.KoreanTime;
import com.postit.mymomsweather.presentation.main.MainActivity;
import com.postit.mymomsweather.R;
import com.postit.mymomsweather.model.EmotionRecord;
import com.postit.mymomsweather.databinding.ActivityCalendarBinding;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import com.prolificinteractive.materialcalendarview.format.DateFormatTitleFormatter;

import org.threeten.bp.format.DateTimeFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {
    final int CALL_LOG_READ_PERMISSION = 1000;
    int[] colorClassArray;
    String[] emotions = {"좋음","슬픔","화남","불안","상처","당황"};

    //int a = ContextCompat.getColor(getApplicationContext(), R.color.happy_pink);

    ActivityCalendarBinding binding;

    private CalendarViewModel model;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setWindowAnimations(0);


        colorClassArray = new int[]{
                ContextCompat.getColor(getApplicationContext(), R.color.happy_pink),
                ContextCompat.getColor(getApplicationContext(), R.color.sad_mint),
                ContextCompat.getColor(getApplicationContext(), R.color.angry_yellow),
                ContextCompat.getColor(getApplicationContext(), R.color.soso_blue),
                ContextCompat.getColor(getApplicationContext(), R.color.wound_green),
                ContextCompat.getColor(getApplicationContext(), R.color.embarrass_gray)
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALL_LOG}, CALL_LOG_READ_PERMISSION);
        }

        model = new ViewModelProvider(this).get(CalendarViewModel.class);
        binding.setVm(model);

        bindPanelButton();
        initDailyPanel();
        initWeeklyPanel();
        initMonthlyPanel();
        initCalendarView();

        model.fetchParentList();

        binding.layoutGohome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        binding.layoutGogame.setOnClickListener((v)->{
            Intent intent = new Intent(this, GameActivity.class);
            startActivity(intent);
        });

    }

    private void bindPanelButton() {
        binding.dailyPanelButton.setOnClickListener((v) -> {
            model.selectedView.setValue(0);
            binding.invalidateAll();
        });
        binding.weeklyPanelButton.setOnClickListener((v) -> {
            model.selectedView.setValue(1);
            binding.invalidateAll();
        });
        binding.monthlyPanelButton.setOnClickListener((v) -> {
            model.selectedView.setValue(2);
            binding.invalidateAll();
        });
    }

    private void initMonthlyPanel() {
        binding.monthlyPieChart.setDrawEntryLabels(true);
        binding.monthlyPieChart.setUsePercentValues(true);
//        binding.monthlyPieChart.setScrollBarSize(12);
        binding.monthlyPieChart.setDescription(null);
        binding.monthlyPieChart.setCenterTextSize(20);
        binding.monthlyPieChart.setEntryLabelTextSize(12);

        model.monthlyEmotionStat.observe(this, new Observer<HashMap<Integer, Integer>>() {
            @Override
            public void onChanged(HashMap<Integer, Integer> hm) {
                ArrayList<PieEntry> monthlyData = new ArrayList<>();
                for (int i = 0; i < emotions.length; i++) {
                monthlyData.add(new PieEntry(hm.getOrDefault(i, 0), emotions[i]));
                }PieDataSet pieDataSet = new PieDataSet(monthlyData, "");
                pieDataSet.setColors(colorClassArray);
                PieData pieData = new PieData(pieDataSet);
                pieData.setValueTextSize(12);
                pieData.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.format("%.2f",value) + "%";
                    }
                });
                pieData.setValueTextColor(Color.WHITE);
                binding.monthlyPieChart.setData(pieData);
                binding.monthlyPieChart.invalidate();

            }
        });

    }

    private void initWeeklyPanel() {
        binding.weeklyPieChart.setDrawEntryLabels(true);
        binding.weeklyPieChart.setUsePercentValues(true);
        binding.weeklyPieChart.setCenterTextSize(20);
        binding.weeklyPieChart.setDescription(null);
        binding.weeklyPieChart.setEntryLabelTextSize(12);
        model.weeklyEmotionStat.observe(this, new Observer<HashMap<Integer, Integer>>() {
            @Override
            public void onChanged(HashMap<Integer, Integer> hm) {
                ArrayList<PieEntry> weeklyData = new ArrayList<>();
                ArrayList<Integer> colors = new ArrayList<>();
                for (int i = 0; i < emotions.length; i++) {
                    if (hm.getOrDefault(i, 0) != 0) {
                        weeklyData.add(new PieEntry(hm.getOrDefault(i, 0), emotions[i]));
                        colors.add(colorClassArray[i]);
                    }
                }
                PieDataSet pieDataSet = new PieDataSet(weeklyData, "");
                pieDataSet.setColors(colors);
                PieData pieData = new PieData(pieDataSet);
                pieData.setValueTextSize(12);
                pieData.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.format("%.2f",value) + "%";
                    }
                });

                pieData.setValueTextColor(Color.WHITE);
                binding.weeklyPieChart.setData(pieData);
                binding.weeklyPieChart.invalidate();

            }
        });

    }

    private void initDailyPanel() {
        model.yesterdayEmotion.observe(this, new Observer<EmotionRecord>() {
            @Override
            public void onChanged(EmotionRecord emotionRecords) {
                if (emotionRecords != null) {
                    switch (emotionRecords.getEmotion()) {
                        case 0:
                            binding.dailyEmotionView.setImageResource(R.drawable.ic_weather_good);
                            binding.dailyEmotionTextView.setText("좋아요");
                            break;
                        case 1:
                            binding.dailyEmotionView.setImageResource(R.drawable.ic_weather_soso);
                            binding.dailyEmotionTextView.setText("슬픔");
                            break;
                        case 2:
                            binding.dailyEmotionView.setImageResource(R.drawable.ic_weather_angry);
                            binding.dailyEmotionTextView.setText("화나요");
                            break;
                        case 3:
                            binding.dailyEmotionView.setImageResource(R.drawable.ic_weather_sad);
                            binding.dailyEmotionTextView.setText("불안");
                            break;
                        case 4:
                            binding.dailyEmotionView.setImageResource(R.drawable.ic_weather_wound);
                            binding.dailyEmotionTextView.setText("상처");
                            break;
                        case 5:
                            binding.dailyEmotionView.setImageResource(R.drawable.ic_weather_embarassed);
                            binding.dailyEmotionTextView.setText("당황");
                            break;
                        default:
                            binding.dailyEmotionView.setImageResource(R.drawable.ic_weather_nono);
                            binding.dailyEmotionTextView.setText("     어제의 \n기록이 없어요");
                    }
                } else {
                    binding.dailyEmotionView.setImageResource(R.drawable.ic_weather_nono);
                    binding.dailyEmotionTextView.setText("     어제의 \n기록이 없어요");
                }

            }
        });
    }

    private void initCalendarView() {
        binding.calendarView.setTitleFormatter(new DateFormatTitleFormatter(DateTimeFormatter.ofPattern("yyyy년 MM월")));
        binding.calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                Log.d("calendar", String.valueOf(day.getDate().toEpochDay()));
                if (model._dayCall.getValue() == null) return false;
                Long callDurationOnDay = model._dayCall.getValue().get(day.getDate().toEpochDay());
                return callDurationOnDay != null && callDurationOnDay >= 3600;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(AppCompatResources
                        .getDrawable(getApplicationContext(), R.drawable.ic_baseline_circle_24_3600));
            }
        });
        binding.calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                Log.d("calendar", String.valueOf(day.getDate().toEpochDay()));
                if (model._dayCall.getValue() == null) return false;
                Long callDurationOnDay = model._dayCall.getValue().get(day.getDate().toEpochDay());
                return callDurationOnDay != null && callDurationOnDay >= 1800 && callDurationOnDay < 3600;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(AppCompatResources
                        .getDrawable(getApplicationContext(), R.drawable.ic_baseline_circle_24_1800));
            }
        });
        binding.calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                Log.d("calendar", String.valueOf(day.getDate().toEpochDay()));
                if (model._dayCall.getValue() == null) return false;
                Long callDurationOnDay = model._dayCall.getValue().get(day.getDate().toEpochDay());
                return callDurationOnDay != null && callDurationOnDay >= 300 && callDurationOnDay < 1800;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(AppCompatResources
                        .getDrawable(getApplicationContext(), R.drawable.ic_baseline_circle_24_300));
            }
        });
        binding.calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                Log.d("calendar", String.valueOf(day.getDate().toEpochDay()));
                if (model._dayCall.getValue() == null) return false;
                Long callDurationOnDay = model._dayCall.getValue().get(day.getDate().toEpochDay());
                return callDurationOnDay != null && callDurationOnDay > 0 && callDurationOnDay < 300;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(AppCompatResources
                        .getDrawable(getApplicationContext(), R.drawable.ic_baseline_circle_24_0));
            }
        });

        binding.calendarView.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                widget.invalidateDecorators();
            }
        });

        model._dayCall.observe(this, new Observer<HashMap<Long, Long>>() {
            @Override
            public void onChanged(HashMap<Long, Long> longLongHashMap) {
                Log.d("calendar", "data change observed");
                binding.calendarView.invalidateDecorators();
//어제

                Date currentTime = Calendar.getInstance().getTime();
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy년 MM월 dd일 EE요일", Locale.KOREAN);
                String date_text = sdf1.format(currentTime);

                Calendar c = Calendar.getInstance();
                try {
                    c.setTime(sdf1.parse(date_text));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                c.add(Calendar.DATE, -1);

                binding.tvDescriptionDaily.setText(sdf1.format(c.getTime()));

                binding.todayTell.setText("어제 통화시간");

                long today_call = longLongHashMap.getOrDefault(KoreanTime.koreaToday()-1, 0L);
                String today_call_str = String.format("%02d:%02d:%02d", today_call/3600, today_call%3600/60, today_call%3600%60);
//                String today_call_str = String.valueOf(today_call/3600) + ":" + String.valueOf(today_call%3600/60)
//                        + ":" + String.valueOf(today_call%3600%60);
                binding.dailyCallLineChart.setText(today_call_str);

                //최근 7일 (단위 : 분)

                Calendar calendar7 = Calendar.getInstance();
                calendar7.add(Calendar.DAY_OF_MONTH, -7);
                Date date_7 = calendar7.getTime();
                String date_7_str = new SimpleDateFormat("yyyy년 MM월 dd일 EE요일", Locale.KOREAN).format(date_7);


                Calendar calendar1 = Calendar.getInstance();
                calendar1.add(Calendar.DAY_OF_MONTH, -1);
                Date date_1 = calendar1.getTime();
                String date_1_str = new SimpleDateFormat("yyyy년 MM월 dd일 EE요일", Locale.KOREAN).format(date_1);

                binding.tvDescriptionWeekly.setText(date_7_str+" \n~ "+date_1_str);


                ArrayList<Entry> values = new ArrayList<>();
                long[] weekly_arr = new long[7];
                for(int i = 0; i < weekly_arr.length; i++){
                    weekly_arr[i] = (longLongHashMap.getOrDefault(KoreanTime.koreaToday()-(7-i), 0L))/60;
                }
                for (int i = 0; i < weekly_arr.length; i++) {
                    values.add(new Entry(i, weekly_arr[i]));
                }

                LineDataSet set1;
                set1 = new LineDataSet(values, "통화량(단위 : 분)");

                ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(set1); // add the data sets

                // create a data object with the data sets
                LineData data = new LineData(dataSets);

                binding.weeklyCallLineChart.setBackgroundColor(Color.WHITE); // 그래프 배경 색 설정
                set1.setColor(Color.BLACK); // 차트의 선 색 설정
                set1.setCircleColor(Color.BLACK); // 차트의 points 점 색 설정

                set1.setDrawFilled(false); // 차트 아래 fill(채우기) 설정
                set1.setFillColor(Color.BLACK); // 차트 아래 채우기 색 설정

                ValueFormatter vf = new ValueFormatter() { //value format here, here is the overridden method
                    @Override
                    public String getFormattedValue(float value) {
                        return ""+(int)value;
                    }
                };
                set1.setValueFormatter(vf);
                set1.setValueTextSize(12f);

                XAxis xAxis = binding.weeklyCallLineChart.getXAxis();
                //Set the value formatter

                String[] x_arr = {"7", "6", "5", "4", "3", "2", "1일전"};
                xAxis.setValueFormatter(new IndexAxisValueFormatter(x_arr));
                // set data
                binding.weeklyCallLineChart.getAxisRight().setEnabled(false);
                binding.weeklyCallLineChart.setDescription(null);
                binding.weeklyCallLineChart.setData(data);


                //이번 달

                String date_text_month = new SimpleDateFormat("yyyy년 MM월", Locale.KOREAN).format(currentTime);
                binding.tvDescriptionMonthly.setText(date_text_month);



                ArrayList<Entry> values2 = new ArrayList<>();

//                long now = System.currentTimeMillis();
//                Date today_date = new Date(now);
//                SimpleDateFormat sdf_day = new SimpleDateFormat("dd");
//                SimpleDateFormat sdf_entire = new SimpleDateFormat("yyyy-MM-dd");
//                int today_day = Integer.valueOf(sdf_day.format(today_date).toString());
//                Log.d("오늘 일", String.valueOf(today_day));
//                Log.d("오늘 일", sdf_entire.format(today_date));
//                long first_day_of_this_month = now - 86400*1000*(today_day-1);
//                //long first_day_of_this_month = now - 86400*1000*(24);
//                Date first_day = new Date(first_day_of_this_month);
//                int first_day_day = Integer.valueOf(sdf_day.format(first_day));
//                Log.d("이 달 1일", String.valueOf(first_day_day));
//                Log.d("이 달 1일", sdf_entire.format(first_day));
                //first_day_of_this_month += 32400000;


                long now = System.currentTimeMillis();


                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_EE");
                SimpleDateFormat sdf_day = new SimpleDateFormat("dd");

                Date today = new Date(now);
                Date first_day = new Date(today.getYear(), today.getMonth(), 1);
                Log.d("today", today+"");
                Log.d("first_day", first_day+"");

                int today_day = Integer.valueOf(sdf_day.format(today).toString());

                Log.d("today_day", today_day+"");
                Log.d("today_sdf", sdf.format(today));

                int first_day_day = Integer.valueOf(sdf_day.format(first_day).toString());
                Log.d("first_day_day", first_day_day+"");
                Log.d("first_day_sdf", sdf.format(first_day));

                long[] monthly_arr = new long[today_day];
                long first_day_epoch = first_day.getTime()/1000/60/60/24;
                Log.d("first_day_epoch", first_day_epoch+"");
                for(int i = 0; i < monthly_arr.length; i++){
                    monthly_arr[i] = (longLongHashMap.getOrDefault(first_day_epoch + i, 0L))/60;
                    Log.d("monthly_arr", String.valueOf(first_day_epoch + i+(longLongHashMap.getOrDefault(first_day_epoch + i*1000*60*60*24, 0L))/60));
                }
                for (int i = 0; i < monthly_arr.length; i++) {
                    values2.add(new Entry(i, monthly_arr[i]));
                    Log.d("monthly_arr", i + ", " + String.valueOf(monthly_arr[i]));
                }

                LineDataSet set2;
                set2 = new LineDataSet(values2, "통화량(단위 : 분)");

                ArrayList<ILineDataSet> dataSets2 = new ArrayList<>();
                dataSets2.add(set2); // add the data sets

                // create a data object with the data sets
                LineData data2 = new LineData(dataSets2);

                binding.monthlyCallLineChart.setBackgroundColor(Color.WHITE); // 그래프 배경 색 설정
                set2.setColor(Color.BLACK); // 차트의 선 색 설정
                set2.setCircleColor(Color.BLACK); // 차트의 points 점 색 설정

                set2.setDrawFilled(false); // 차트 아래 fill(채우기) 설정
                set2.setFillColor(Color.BLACK); // 차트 아래 채우기 색 설정

                ValueFormatter vf2 = new ValueFormatter() { //value format here, here is the overridden method
                    @Override
                    public String getFormattedValue(float value) {
                        return ""+(int)value;
                    }
                };
                set2.setValueFormatter(vf2);
                set2.setValueTextSize(12f);

                XAxis xAxis2 = binding.monthlyCallLineChart.getXAxis();
                //Set the value formatter
                xAxis2.setEnabled(false);
//                String[] x_arr2 = {"7", "6", "5", "4", "3", "2", "1일전"};
//                xAxis.setValueFormatter(new IndexAxisValueFormatter(x_arr));
                // set data
                binding.monthlyCallLineChart.getAxisRight().setEnabled(false);
                binding.monthlyCallLineChart.setDescription(null);
                binding.monthlyCallLineChart.setData(data2);
                }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CALL_LOG_READ_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    model.fetchCallHistory();
                }
        }
    }

}
