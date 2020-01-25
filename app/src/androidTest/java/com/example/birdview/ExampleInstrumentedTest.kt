package com.example.birdview

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4

import androidx.test.rule.ActivityTestRule
import kotlinx.android.synthetic.main.activity_main.*
import org.hamcrest.Matchers.not
import org.junit.*

import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.runners.MethodSorters


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ExampleInstrumentedTest {
    @Rule
    @JvmField
    val activityRule = ActivityTestRule(MainActivity::class.java, true, true)
    private var mIdle=SimpleIdlingResource("async")
    @Before
    fun registerIdle(){
        IdlingRegistry.getInstance().register(mIdle)
    }

    @Test
    //스크롤에 따라 자동 로드 되어야합니다. (20개씩)
    //목록 아래로 이동할 때 사라지고, 위로 이동할 때 나타나는 헤더를 구현하세요.
    fun a_loadItemAndHideSpinner(){

        var itemCnt:Int
        onIdle()
        itemCnt=activityRule.activity.recyclerView.adapter!!.itemCount
        assertEquals(20, itemCnt)
        onView(withId(R.id.spinner_container)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerView)).perform(swipeUp())
        onView(withId(R.id.recyclerView)).perform(swipeUp())
        onView(withId(R.id.recyclerView)).perform(swipeUp())
        onView(withId(R.id.spinner_container)).check(matches(not(isDisplayed())))
        onIdle()
        itemCnt=activityRule.activity.recyclerView.adapter!!.itemCount
        assertEquals(40, itemCnt)

    }

    @Test
    //헤더의 필터를 선택할 경우, 선택한 피부 타입에 대한 점수순으로 정렬되어야 합니다.
    fun b_filterItem(){
        onView(withId(R.id.spinner)).perform(click())
        onView(withText("지성")).perform(click())
        onIdle()
        var cache=activityRule.activity.getCache()
        var flag=cache.zipWithNext().all { it.first.oily_score>=it.second.oily_score }
        assertEquals(true, flag)


        onView(withId(R.id.spinner)).perform(click())
        onView(withText("건성")).perform(click())
        onIdle()
        cache=activityRule.activity.getCache()
        flag=cache.zipWithNext().all { it.first.dry_score>=it.second.dry_score }
        assertEquals(true, flag)

        onView(withId(R.id.spinner)).perform(click())
        onView(withText("민감성")).perform(click())
        onIdle()
        cache=activityRule.activity.getCache()
        flag=cache.zipWithNext().all { it.first.sensitive_score>=it.second.sensitive_score }
        assertEquals(true, flag)
    }

    @Test
    //검색 창에 키워드를 입력하여 상품을 검색할 수 있습니다.
    fun c_searchItem(){
        onView(withId(R.id.search_text_view)).perform(typeText("ml"))
        onView(withId(R.id.searchBtn)).perform(click())
        onIdle()
        val cache=activityRule.activity.getCache()
        val flag=cache.all { it.title.contains("ml") }
        assertEquals(true,flag)
    }

    @Test
    //X버튼 클릭시 상품 목록으로 이동
    fun d_detailViewTestWithExitBtn(){
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
            click()))
        onIdle()
        onView(withId(R.id.detail_window)).inRoot(RootMatchers.isPlatformPopup()).check(matches(isDisplayed()))
        onView(withId(R.id.exitBtn)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
        onView(withId(R.id.detail_window)).check(doesNotExist())
    }

    @Test
    //뒤로가기 버튼 클릭 시 상품 목록으로 돌아갈 수 있습니다.
    fun e_detailViewTestWithBackpressBtn(){
        onView(withId(R.id.recyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0,
            click()))
        onIdle()
        onView(withId(R.id.detail_window)).inRoot(RootMatchers.isPlatformPopup()).check(matches(isDisplayed()))
        onView(isRoot()).perform(ViewActions.pressBack())
        onView(withId(R.id.detail_window)).check(doesNotExist())
    }

    @After
    fun unregisterIdle(){
        IdlingRegistry.getInstance().unregister(mIdle)
    }

}
