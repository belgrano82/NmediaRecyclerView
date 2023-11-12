package ru.netology.nmedia.viewmodel

import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.lifecycle.*
import androidx.paging.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.TimeSeparator
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import javax.inject.Inject
import kotlin.random.Random

private val empty = Post(
    id = 0,
    content = "",
    authorId = 0,
    author = "",
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = 0,
)

private val noPhoto = PhotoModel()

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val postDao: PostDao,
    auth: AppAuth,
) : ViewModel() {
//    private val cached: Flow<PagingData<FeedItem>> = repository
//        .data
//        .map { pagingData ->
//            pagingData
//                .insertSeparators { before, after ->
//                    if (after?.published!! <= 1699450000L && before?.published!! > 1699450000L) {
//                        TimeSeparator(Random.nextLong(), "НА ПРОШЛОЙ НЕДЕЛЕ")
//                    }
//
//                    if (after.published <= 1699600000L && before?.published!! > 1699600000L ) {
//                        TimeSeparator(Random.nextLong(), "ВЧЕРА")
//                    } else if (after.published <= 2000000000L && before == null) {
//                        TimeSeparator(Random.nextLong(), "СЕГОДНЯ")
//
//                    }else if (before?.id?.rem(5) == 0L) {
//                        Ad(
//                            Random.nextLong(),
//                            "https://netology.ru",
//                            "figma.jpg"
//                        )
//                    } else {
//                        null
//                    }
//                }
//
//        }
//        .cachedIn(viewModelScope)


    private val cached: Flow<PagingData<FeedItem>> = repository
        .data
        .map { pagingData ->
            pagingData.insertSeparators(
                generator = { before, after ->

                   if (after == null) {
                       return@insertSeparators null
                   }
                    if (before == null) {
//                         we're at the beginning of the list
                      return@insertSeparators  TimeSeparator(Random.nextLong(), "СЕГОДНЯ")
                    }



                    if (after?.id?.equals(8L) == true) {
                        TimeSeparator(Random.nextLong(), "НА ПРОШЛОЙ НЕДЕЛЕ")
                    } else if (after?.id?.equals(10L) == true) {
                        TimeSeparator(Random.nextLong(), "ВЧЕРА")

                    } else {
                        null
                    }


                })
                .insertSeparators { before: FeedItem?, _ ->
                    if (before?.id?.rem(5) != 0L) null else
                        Ad(
                            Random.nextLong(),
                            "https://netology.ru",
                            "figma.jpg"
                        )
                }
        }
        .cachedIn(viewModelScope)


    val data: Flow<PagingData<FeedItem>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            cached
                .map { pagingData ->
                    pagingData.map { item ->
                        if (item !is Post) item else item.copy(ownedByMe = item.authorId == myId)
                    }
                }
        }

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo



    init {
        loadPosts()
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            // repository.stream.cachedIn(viewModelScope).
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun refreshPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(refreshing = true)
//            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun save() {
        edited.value?.let {
            viewModelScope.launch {
                try {
                    repository.save(
                        it, _photo.value?.uri?.let { MediaUpload(it.toFile()) }
                    )

                    _postCreated.value = Unit
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        edited.value = empty
        _photo.value = noPhoto
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun changePhoto(uri: Uri?) {
        _photo.value = PhotoModel(uri)
    }

    fun likeById(id: Long) {
        TODO()
    }

    fun removeById(id: Long) {
        TODO()
    }
}
