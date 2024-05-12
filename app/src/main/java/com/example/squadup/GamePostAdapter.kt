import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.squadup.GamePost
import com.example.squadup.GamePostPopup
import com.example.squadup.R
import java.io.IOException
import java.util.*

class GamePostAdapter(
    private val context: Context,
    private val gamePosts: List<GamePost>
) : RecyclerView.Adapter<GamePostAdapter.GamePostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GamePostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.game_post_card, parent, false)
        return GamePostViewHolder(view)
    }

    override fun onBindViewHolder(holder: GamePostViewHolder, position: Int) {
        val gamePost = gamePosts[position]

        holder.sportTypeTextView.text = gamePost.sportType
        holder.numPlayersTextView.text = "Players: ${gamePost.numPlayers}"
        holder.timeframeTextView.text = "Timeframe: ${gamePost.timeframe}"
        holder.numPlayersResponded.text = "Players Responded: ${gamePost.numPlayersResponded}"

        val location = gamePost.location
        val latitude = location["latitude"] ?: 0.0
        val longitude = location["longitude"] ?: 0.0
        val address = getAddressFromCoordinates(latitude, longitude)  // Converting to address
        holder.locationTextView.text = address

        val userInfo = gamePost.userInfo
        holder.userNameTextView.text = userInfo["displayName"] ?: "Anonymous"
        val profilePicUri = userInfo["profilePicUri"]

        if (profilePicUri != null && profilePicUri.isNotEmpty()) {
            Glide.with(context)
                .load(Uri.parse(profilePicUri))
                .placeholder(R.drawable.profile_pic_placeholder)
                .error(R.drawable.profile_pic_placeholder)
                .into(holder.userProfilePicImageView)
        } else {
            holder.userProfilePicImageView.setImageResource(R.drawable.profile_pic_placeholder)
        }

        // Set click listener to open GamePostDetailsDialogFragment
        holder.itemView.setOnClickListener {
            val dialog = GamePostPopup().apply {
                arguments = Bundle().apply {
                    putString("POST_ID", gamePost.id)
                    putString("sportType", gamePost.sportType)
                    putString("numPlayers", gamePost.numPlayers.toString())
                    putString("numPlayersResponded", gamePost.numPlayersResponded.toString())
                    putString("timeframe", gamePost.timeframe)
                    putString("authorName", userInfo["displayName"])
                    putString("authorImageUrl", profilePicUri)
                    putString("address", address)  // Passing address to dialog
                    putDouble("latitude", latitude)
                    putDouble("longitude", longitude)
                }
            }
            dialog.show((context as FragmentActivity).supportFragmentManager, "GamePostDetails")
        }
    }

    override fun getItemCount(): Int = gamePosts.size

    private fun getAddressFromCoordinates(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
            if (addresses.isNotEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                "Unknown Location"
            }
        } catch (e: IOException) {
            "Unknown Location"
        }
    }

    class GamePostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sportTypeTextView: TextView = view.findViewById(R.id.post_sport_type)
        val numPlayersTextView: TextView = view.findViewById(R.id.post_num_players)
        val numPlayersResponded : TextView = view.findViewById(R.id.text_view_num_responded)
        val timeframeTextView: TextView = view.findViewById(R.id.post_timeframe)
        val locationTextView: TextView = view.findViewById(R.id.post_location)
        val userNameTextView: TextView = view.findViewById(R.id.post_user_name)
        val userProfilePicImageView: ImageView = view.findViewById(R.id.post_user_profile_pic)
    }
}



